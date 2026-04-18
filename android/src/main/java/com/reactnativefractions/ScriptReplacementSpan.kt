package com.reactnativefractions

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.style.ReplacementSpan
import kotlin.math.ceil

/**
 * Inline superscript / subscript span. Content is rendered at 0.65× the
 * host font size with the baseline raised or lowered. Content may itself
 * contain a {@link RunNode.Fraction}, in which case the fraction is drawn
 * as a shrunk stacked exponent (matches the reference `25^(-3/2)`
 * rendering from the workbook).
 *
 * Library schema 0.3.0+. Emitted by apps that send `type: "superscript"`
 * or `type: "subscript"` runs to the `FractionText` component.
 */
class ScriptReplacementSpan internal constructor(
  private val content: List<RunNode>,
  private val hostTextSizePx: Float,
  private val textColor: Int,
  private val typeface: Typeface?,
  private val fontWeight: Int,
  private val barThicknessPxOverride: Float?,
  private val raise: Boolean,
) : ReplacementSpan() {

  private val node: RunNode =
    if (raise) RunNode.Superscript(content) else RunNode.Subscript(content)

  override fun getSize(
    paint: Paint,
    text: CharSequence?,
    start: Int,
    end: Int,
    fm: Paint.FontMetricsInt?,
  ): Int {
    val metrics = FractionRenderer.measureRun(
      node,
      hostTextSizePx,
      typeface,
      fontWeight,
      barThicknessPxOverride,
    )
    if (fm != null) {
      val hostFm = paint.fontMetricsInt
      fm.ascent = minOf(hostFm.ascent, -ceil(metrics.ascent).toInt())
      fm.top = minOf(hostFm.top, fm.ascent)
      fm.descent = maxOf(hostFm.descent, ceil(metrics.descent).toInt())
      fm.bottom = maxOf(hostFm.bottom, fm.descent)
    }
    return ceil(metrics.width).toInt()
  }

  override fun draw(
    canvas: Canvas,
    text: CharSequence?,
    start: Int,
    end: Int,
    x: Float,
    top: Int,
    y: Int,
    bottom: Int,
    paint: Paint,
  ) {
    FractionRenderer.drawRun(
      canvas = canvas,
      run = node,
      x = x,
      baselineY = y.toFloat(),
      hostTextSizePx = hostTextSizePx,
      typeface = typeface,
      fontWeight = fontWeight,
      textColor = textColor,
      barThicknessPxOverride = barThicknessPxOverride,
    )
  }
}
