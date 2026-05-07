package com.example.tooltipguide.tooltip

import android.app.Activity
import android.util.Log
import android.view.View
import com.example.tooltipguide.models.TooltipConfig
import com.example.tooltipguide.models.TooltipStep
import com.google.gson.Gson

/**
 * Orchestrates a multi-step tooltip tour.
 *
 * Usage:
 * ```
 * val manager = TooltipManager(activity, jsonString)
 * manager.onTourCompleted = { /* e.g. show a Snackbar */ }
 * manager.start()
 * ```
 *
 * The JSON is parsed once in the constructor; each call to [start] begins
 * from step 0.  Calling [finish] at any point tears down the current tooltip
 * and resets the index.
 */
class TooltipManager(
    private val activity: Activity,
    jsonConfig: String
) {

    companion object {
        private const val TAG = "TooltipManager"
    }

    // ── Parsed config ───────────────────────────────────────────────────
    private val config: TooltipConfig =
        Gson().fromJson(jsonConfig, TooltipConfig::class.java)

    // ── State ───────────────────────────────────────────────────────────
    private var currentStepIndex = 0
    private var currentOverlay: TooltipOverlayView? = null
    private var isRunning = false

    // ── Public callback ── ★ FIX #8: completion callback ────────────────
    /** Invoked after the user taps the CTA on the very last step. */
    var onTourCompleted: (() -> Unit)? = null

    // ═══════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════

    /** Begin the tour from the first step. */
    fun start() {
        if (config.steps.isEmpty()) {
            Log.w(TAG, "start() called but the config has 0 steps.")
            return
        }
        currentStepIndex = 0
        isRunning = true
        showStep(currentStepIndex)
    }

    /** Tear down any visible tooltip and reset. */
    fun finish() {
        isRunning = false
        currentOverlay?.dismiss()
        currentOverlay = null
        currentStepIndex = 0
    }

    /** @return `true` while a tour is actively being displayed. */
    fun isActive(): Boolean = isRunning

    // ═══════════════════════════════════════════════════════════════════
    // Internal
    // ═══════════════════════════════════════════════════════════════════

    private fun showStep(index: Int) {
        // All steps complete → fire callback
        if (index >= config.steps.size) {
            finish()
            onTourCompleted?.invoke()
            return
        }

        val step: TooltipStep = config.steps[index]

        // ★ FIX #3: getIdentifier returns 0 when the name doesn't match any id.
        //           Calling findViewById(0) on some OEMs can return a random
        //           internal view, so we must guard against 0 explicitly.
        val resId = activity.resources.getIdentifier(
            step.target, "id", activity.packageName
        )

        if (resId == 0) {
            Log.w(TAG, "Target view \"${step.target}\" not found – skipping step ${step.id}")
            currentStepIndex++
            showStep(currentStepIndex)
            return
        }

        val targetView: View? = activity.findViewById(resId)

        if (targetView == null || !targetView.isShown) {
            Log.w(TAG, "Target view \"${step.target}\" exists but is not visible – skipping step ${step.id}")
            currentStepIndex++
            showStep(currentStepIndex)
            return
        }

        // Dismiss previous overlay (if any) before creating a new one
        currentOverlay?.dismiss()

        currentOverlay = TooltipOverlayView(activity).apply {
            setup(step, targetView, index, config.steps.size)
            onCtaClicked = {
                currentStepIndex++
                showStep(currentStepIndex)
            }
            show()
        }
    }
}
