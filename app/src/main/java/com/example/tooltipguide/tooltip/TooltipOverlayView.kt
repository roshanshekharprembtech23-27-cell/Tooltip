package com.example.tooltipguide.tooltip

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.tooltipguide.models.TooltipStep

/**
 * Full-screen overlay that:
 *   1. Dims the entire screen with a semi-transparent backdrop.
 *   2. "Punches a hole" around the target view using an even-odd Path
 *      (works reliably on ALL devices, unlike PorterDuff.Mode.CLEAR).
 *   3. Draws a triangular arrow pointing from the tooltip card to the target.
 *   4. Shows a styled tooltip card with title, subtitle, step counter, and CTA button.
 *
 * Lifecycle:  setup() → show() → [user clicks CTA] → dismiss()
 */
@SuppressLint("ViewConstructor")
class TooltipOverlayView(
    private val activity: Activity
) : FrameLayout(activity) {

    // ── Public callbacks ────────────────────────────────────────────────────
    var onCtaClicked: (() -> Unit)? = null

    // ── Internal state ──────────────────────────────────────────────────────
    private var targetView: View? = null
    private var cardView: CardView? = null
    private var storedStep: TooltipStep? = null
    private var stepIndex: Int = 0
    private var totalSteps: Int = 0

    // ── Paints ──────────────────────────────────────────────────────────────
    private val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // ── Geometry ─────────────────────────────────────────────────────────────
    private val targetRect = RectF()
    private val arrowPath = Path()
    private val backdropPath = Path()

    // ── Defaults ─────────────────────────────────────────────────────────────
    companion object {
        private const val DEF_BACKDROP_OPACITY = 0.65f
        private const val DEF_ARROW_COLOR = "#FFFFFF"
        private const val DEF_CTA_BG = "#007BFF"
        private const val DEF_CTA_TEXT = "#FFFFFF"
        private const val DEF_CTA_RADIUS = 24f
        private const val DEF_TITLE_COLOR = "#1B1B1F"
        private const val DEF_TITLE_SIZE = 18f
        private const val DEF_SUBTITLE_COLOR = "#49454F"
        private const val DEF_SUBTITLE_SIZE = 14f
        private const val DEF_CARD_BG = "#FFFFFF"
        private const val DEF_CARD_RADIUS = 16f
        private const val HIGHLIGHT_PADDING_DP = 8f
        private const val ARROW_SIZE_DP = 10f
        private const val CARD_MARGIN_DP = 16f
        private const val FADE_DURATION_MS = 250L
    }

    init {
        setWillNotDraw(false)       // we draw custom backdrop + arrow
        isClickable = true          // ★ FIX #4: block taps from reaching views below
        isFocusable = true
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Configure this overlay for a specific tooltip step.
     * Call [show] afterwards to attach it to the window.
     */
    fun setup(step: TooltipStep, target: View, currentStep: Int, total: Int) {
        storedStep = step
        targetView = target
        stepIndex = currentStep
        totalSteps = total

        val s = step.styling

        // Backdrop opacity
        backdropPaint.alpha = ((s?.backdropOpacity ?: DEF_BACKDROP_OPACITY) * 255).toInt()

        // Arrow colour
        arrowPaint.color = Color.parseColor(s?.arrowColor ?: DEF_ARROW_COLOR)

        // Build the card (but don't position it yet – we need a layout pass first)
        buildCard(step)
    }

    /**
     * Attach overlay to the Activity's DecorView and fade in.
     */
    fun show() {
        val decor = activity.window.decorView as ViewGroup
        decor.addView(this, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // ★ FIX #2: wait for the first real layout pass so cardView.height > 0
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                positionCardAndArrow()
                // ★ FIX #6: smooth fade-in
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(FADE_DURATION_MS)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        })
    }

    /**
     * Safely detach from the window (won't crash if already removed).
     */
    fun dismiss() {
        // ★ FIX #5: guard against double-dismiss or dismiss before attach
        if (!isAttachedToWindow) return
        val decor = activity.window.decorView as ViewGroup
        decor.removeView(this)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Card construction
    // ═══════════════════════════════════════════════════════════════════════

    @SuppressLint("SetTextI18n")
    private fun buildCard(step: TooltipStep) {
        val s = step.styling

        val card = CardView(context).apply {
            radius = dpToPx(s?.cardRadiusDp ?: DEF_CARD_RADIUS)
            setCardBackgroundColor(Color.parseColor(s?.cardBackground ?: DEF_CARD_BG))
            cardElevation = dpToPx(6f)
            // Start invisible; positionCardAndArrow() will reveal it
            visibility = View.INVISIBLE
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(20f).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // ── Step indicator (e.g. "Step 1 of 3") ── ★ FIX #7
        val stepIndicator = TextView(context).apply {
            text = "Step ${stepIndex + 1} of $totalSteps"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#9E9E9E"))
            setPadding(0, 0, 0, dpToPx(6f).toInt())
        }

        // ── Title ──
        val titleTv = TextView(context).apply {
            text = step.title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, s?.titleSizeSp ?: DEF_TITLE_SIZE)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(s?.titleColor ?: DEF_TITLE_COLOR))
        }

        // ── Subtitle ──
        val subtitleTv = TextView(context).apply {
            text = step.subtitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, s?.subtitleSizeSp ?: DEF_SUBTITLE_SIZE)
            setTextColor(Color.parseColor(s?.subtitleColor ?: DEF_SUBTITLE_COLOR))
            setPadding(0, dpToPx(8f).toInt(), 0, dpToPx(20f).toInt())
            setLineSpacing(dpToPx(2f), 1f)
        }

        // ── CTA Button ──
        val ctaBtn = TextView(context).apply {
            text = step.cta
            setTextColor(Color.parseColor(s?.ctaTextColor ?: DEF_CTA_TEXT))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, Typeface.BOLD)
            val hPad = dpToPx(28f).toInt()
            val vPad = dpToPx(10f).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            gravity = Gravity.CENTER

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(s?.ctaRadiusDp ?: DEF_CTA_RADIUS)
                setColor(Color.parseColor(s?.ctaBackground ?: DEF_CTA_BG))
            }
            background = bg
            isClickable = true
            isFocusable = true

            setOnClickListener { onCtaClicked?.invoke() }
        }

        container.addView(stepIndicator)
        container.addView(titleTv)
        container.addView(subtitleTv)

        val ctaParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.END }
        container.addView(ctaBtn, ctaParams)

        card.addView(container)

        // Width = screen width minus left+right margin
        val cardLp = LayoutParams(
            resources.displayMetrics.widthPixels - dpToPx(CARD_MARGIN_DP * 2).toInt(),
            LayoutParams.WRAP_CONTENT
        )
        addView(card, cardLp)
        cardView = card
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Positioning logic (runs AFTER layout so dimensions are real)
    // ═══════════════════════════════════════════════════════════════════════

    private fun positionCardAndArrow() {
        val target = targetView ?: return
        val card = cardView ?: return

        // ── 1. Compute highlight rect in window coordinates ──
        val loc = IntArray(2)
        target.getLocationInWindow(loc)
        val pad = dpToPx(HIGHLIGHT_PADDING_DP)

        targetRect.set(
            loc[0].toFloat() - pad,
            loc[1].toFloat() - pad,
            loc[0].toFloat() + target.width + pad,
            loc[1].toFloat() + target.height + pad
        )

        // ── 2. Decide: card above or below the target? ──
        val arrowH = dpToPx(ARROW_SIZE_DP)
        val gap = dpToPx(4f)                // breathing room between arrow tip and highlight
        val screenH = resources.displayMetrics.heightPixels.toFloat()

        val spaceBelow = screenH - targetRect.bottom - arrowH - gap
        val spaceAbove = targetRect.top - arrowH - gap

        val arrowPos = storedStep?.styling?.arrowPosition ?: "auto"
        val placeBelow = when (arrowPos) {
            "bottom" -> true
            "top" -> false
            else -> spaceBelow >= card.height       // auto: prefer below if it fits
        }

        // ── 3. Horizontal: centre on target, clamp to screen ──
        val margin = dpToPx(CARD_MARGIN_DP)
        val cardW = card.width.toFloat()
        var cx = targetRect.centerX() - cardW / 2f
        cx = cx.coerceIn(margin, resources.displayMetrics.widthPixels - cardW - margin)

        card.translationX = cx

        // ── 4. Vertical ──
        if (placeBelow) {
            card.translationY = targetRect.bottom + arrowH + gap
        } else {
            card.translationY = targetRect.top - arrowH - gap - card.height
        }

        // ── 5. Arrow triangle ──
        arrowPath.reset()
        val ax = targetRect.centerX()       // arrow tip x-centre

        if (placeBelow) {
            // Arrow points UP toward the target
            val baseY = targetRect.bottom + gap + arrowH
            arrowPath.moveTo(ax, targetRect.bottom + gap)           // tip
            arrowPath.lineTo(ax - arrowH, baseY)                    // bottom-left
            arrowPath.lineTo(ax + arrowH, baseY)                    // bottom-right
        } else {
            // Arrow points DOWN toward the target
            val baseY = targetRect.top - gap - arrowH
            arrowPath.moveTo(ax, targetRect.top - gap)              // tip
            arrowPath.lineTo(ax - arrowH, baseY)                    // top-left
            arrowPath.lineTo(ax + arrowH, baseY)                    // top-right
        }
        arrowPath.close()

        // ── 6. Build backdrop path (even-odd → hole around target) ── ★ FIX #1
        backdropPath.reset()
        backdropPath.fillType = Path.FillType.EVEN_ODD
        backdropPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        backdropPath.addRoundRect(targetRect, dpToPx(12f), dpToPx(12f), Path.Direction.CCW)

        // Reveal the card now that it's in the right place
        card.visibility = View.VISIBLE
        invalidate()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Drawing
    // ═══════════════════════════════════════════════════════════════════════

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. Dimmed backdrop with transparent hole (no PorterDuff needed)
        canvas.drawPath(backdropPath, backdropPaint)
        // 2. Arrow connecting card to target
        canvas.drawPath(arrowPath, arrowPaint)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Utility
    // ═══════════════════════════════════════════════════════════════════════

    private fun dpToPx(dp: Float): Float =
        dp * resources.displayMetrics.density
}
