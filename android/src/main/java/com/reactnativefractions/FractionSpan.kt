package com.reactnativefractions

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.style.ReplacementSpan
import kotlin.math.ceil

/**
 * Stacked fraction span: numerator, vinculum, denominator inline with host
 * text. Accepts either plain `numerator` / `denominator` strings
 * (library schema `< 0.3.0`, unchanged behaviour) or structured run trees
 * (library schema 0.3.0+, so numerator or denominator may itself contain
 * nested fractions or raised/lowered scripts).
 */
class FractionSpan internal constructor(
  private val numeratorRuns: List<RunNode>,
  private val denominatorRuns: List<RunNode>,
  private val hostTextSizePx: Float,
  private val textColor: Int,
  private val typeface: Typeface?,
  private val fontWeight: Int = 400,
  private val barThicknessPxOverride: Float? = null,
) : ReplacementSpan() {

  /**
   * Backwards-compatible constructor (library schema `< 0.3.0`). Wraps the
   * strings as single-element {@link RunNode.Text} lists and delegates to
   * the primary constructor so older call sites compile unchanged.
   */
  constructor(
    numerator: String,
    denominator: String,
    hostTextSizePx: Float,
    textColor: Int,
    typeface: Typeface?,
    fontWeight: Int = 400,
    barThicknessPxOverride: Float? = null,
  ) : this(
    numeratorRuns = listOf(RunNode.Text(numerator)),
    denominatorRuns = listOf(RunNode.Text(denominator)),
    hostTextSizePx = hostTextSizePx,
    textColor = textColor,
    typeface = typeface,
    fontWeight = fontWeight,
    barThicknessPxOverride = barThicknessPxOverride,
  )

  private val fractionNode = RunNode.Fraction(
    numerator = numeratorRuns,
    denominator = denominatorRuns,
    numeratorString = "",
    denominatorString = "",
  )

  private val xRect = Rect()

  private fun approximateXHeight(hostPaint: Paint): Float {
    hostPaint.getTextBounds("x", 0, 1, xRect)
    val measured = -xRect.top.toFloat()
    if (measured > 0f) return measured
    return hostPaint.textSize * 0.5f
  }

  override fun getSize(
    paint: Paint,
    text: CharSequence?,
    start: Int,
    end: Int,
    fm: Paint.FontMetricsInt?,
  ): Int {
    val metrics = FractionRenderer.measureRun(
      fractionNode,
      hostTextSizePx,
      typeface,
      fontWeight,
      barThicknessPxOverride,
    )
    if (fm != null) {
      val hostFm = paint.fontMetricsInt
      val xHeight = approximateXHeight(paint)
      val midline = -xHeight / 2f
      val neededAscent = ceil(-midline + metrics.ascent - xHeight / 2f).toInt()
      val neededDescent = ceil(midline + metrics.descent + xHeight / 2f).toInt()
      fm.ascent = minOf(hostFm.ascent, -neededAscent)
      fm.top = minOf(hostFm.top, fm.ascent)
      fm.descent = maxOf(hostFm.descent, neededDescent)
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
      run = fractionNode,
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
