package com.reactnativefractions

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.style.ReplacementSpan
import android.util.Log
import kotlin.math.ceil
import kotlin.math.max

/**
 * Stacked fraction span: numerator, vinculum, denominator inline with host text.
 */
class FractionSpan(
  private val numerator: String,
  private val denominator: String,
  private val hostTextSizePx: Float,
  private val textColor: Int,
  private val typeface: Typeface?,
  private val fontWeight: Int = 400,
) : ReplacementSpan() {

  private val fracPaint: TextPaint = TextPaint().apply {
    isAntiAlias = true
    textSize = hostTextSizePx * 0.75f
    color = textColor
  }

  init {
    if (typeface != null) {
      fracPaint.typeface =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          Typeface.create(typeface, fontWeight, false)
        } else {
          typeface
        }
    }
    if (BuildConfig.DEBUG) {
      Log.d(
        "FractionText",
        "FractionSpan typeface=$typeface weight=$fontWeight " +
          "applied=${fracPaint.typeface}",
      )
    }
  }

  private val barPaint: Paint = Paint().apply {
    isAntiAlias = true
    color = textColor
    strokeWidth = max(1f, fracPaint.textSize * 0.06f)
    style = Paint.Style.STROKE
  }

  private val gap: Float = max(1f, fracPaint.textSize * 0.08f)
  private val sidePadding: Float = fracPaint.textSize * 0.2f
  private val xRect = Rect()

  private fun cellWidth(): Float {
    val numW = fracPaint.measureText(numerator)
    val denW = fracPaint.measureText(denominator)
    return max(numW, denW)
  }

  private fun totalWidth(): Float = cellWidth() + sidePadding * 2

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
    if (fm != null) {
      val fracFm = fracPaint.fontMetricsInt
      val fracLineHeight = (fracFm.descent - fracFm.ascent).toFloat()
      val barStroke = barPaint.strokeWidth
      val halfStack = fracLineHeight + gap + barStroke / 2f
      val xHeight = approximateXHeight(paint)
      val midline = -xHeight / 2f
      val neededAscent = ceil(-midline + halfStack).toInt()
      val neededDescent = ceil(midline + halfStack).toInt()
      val hostFm = paint.fontMetricsInt
      fm.ascent = minOf(hostFm.ascent, -neededAscent)
      fm.top = minOf(hostFm.top, fm.ascent)
      fm.descent = maxOf(hostFm.descent, neededDescent)
      fm.bottom = maxOf(hostFm.bottom, fm.descent)
    }
    return ceil(totalWidth()).toInt()
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
    val numW = fracPaint.measureText(numerator)
    val denW = fracPaint.measureText(denominator)
    val cellW = max(numW, denW)

    val fracFm = fracPaint.fontMetrics
    val barY = y.toFloat() - approximateXHeight(paint) / 2f

    val numBaseline = barY - gap - fracFm.descent
    val numX = x + sidePadding + (cellW - numW) / 2f
    canvas.drawText(numerator, numX, numBaseline, fracPaint)

    val barStart = x + sidePadding * 0.5f
    val barEnd = x + sidePadding * 1.5f + cellW
    canvas.drawLine(barStart, barY, barEnd, barY, barPaint)

    val denBaseline = barY + gap - fracFm.ascent
    val denX = x + sidePadding + (cellW - denW) / 2f
    canvas.drawText(denominator, denX, denBaseline, fracPaint)
  }
}
