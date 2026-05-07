package com.example.tooltipguide.models

import com.google.gson.annotations.SerializedName

/**
 * Root JSON wrapper – contains an ordered list of tooltip steps.
 */
data class TooltipConfig(
    val steps: List<TooltipStep>
)

/**
 * A single tooltip step.
 *
 * @param id       Unique identifier for this step.
 * @param target   The android:id name of the view to highlight (e.g. "feedback_input").
 * @param title    Bold heading shown inside the tooltip card.
 * @param subtitle Supporting description below the title.
 * @param cta      Label for the call-to-action button (e.g. "Next", "Got it!").
 * @param styling  Optional visual overrides; falls back to sensible defaults.
 */
data class TooltipStep(
    val id: String,
    val target: String,
    val title: String,
    val subtitle: String,
    val cta: String,
    val styling: TooltipStyling? = null
)

/**
 * Every field is nullable so missing JSON keys simply fall back to defaults
 * inside [com.example.tooltipguide.tooltip.TooltipOverlayView].
 */
data class TooltipStyling(
    // ── Backdrop ──
    @SerializedName("backdropOpacity") val backdropOpacity: Float? = null,

    // ── Arrow ──
    @SerializedName("arrowPosition") val arrowPosition: String? = null,   // "top", "bottom", "auto"
    @SerializedName("arrowColor") val arrowColor: String? = null,

    // ── CTA Button ──
    @SerializedName("ctaBackground") val ctaBackground: String? = null,
    @SerializedName("ctaTextColor") val ctaTextColor: String? = null,
    @SerializedName("ctaRadiusDp") val ctaRadiusDp: Float? = null,

    // ── Title ──
    @SerializedName("titleColor") val titleColor: String? = null,
    @SerializedName("titleSizeSp") val titleSizeSp: Float? = null,

    // ── Subtitle ──
    @SerializedName("subtitleColor") val subtitleColor: String? = null,
    @SerializedName("subtitleSizeSp") val subtitleSizeSp: Float? = null,

    // ── Card ──
    @SerializedName("cardBackground") val cardBackground: String? = null,
    @SerializedName("cardRadiusDp") val cardRadiusDp: Float? = null
)
