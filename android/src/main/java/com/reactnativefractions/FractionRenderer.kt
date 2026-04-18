package com.reactnativefractions

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import kotlin.math.max

/**
 * Stable internal representation of a `TokenRun` (library schema 0.3.0+)
 * converted from the React Native bridge so the renderer can traverse it
 * without re-reading `ReadableMap`s on every draw.
 */
internal sealed class RunNode {
  data class Text(val text: String) : RunNode()
  data class Fraction(
    val numerator: List<RunNode>,
    val denominator: List<RunNode>,
    val numeratorString: String,
    val denominatorString: String,
  ) : RunNode()
  data class Superscript(val content: List<RunNode>) : RunNode()
  data class Subscript(val content: List<RunNode>) : RunNode()
}

/**
 * Converts a bridge `ReadableMap` / `ReadableArray` tree into the immutable
 * {@link RunNode} sealed hierarchy consumed by {@link FractionRenderer}.
 */
internal object RunNodeParser {
  fun parseArray(array: ReadableArray?): List<RunNode> {
    if (array == null) return emptyList()
    val out = ArrayList<RunNode>(array.size())
    for (i in 0 until array.size()) {
      val map = runCatching { array.getMap(i) }.getOrNull() ?: continue
      parseMap(map)?.let { out.add(it) }
    }
    return out
  }

  fun parseMap(map: ReadableMap): RunNode? {
    val type = map.getString("type") ?: return null
    return when (type) {
      "text" -> RunNode.Text(map.getString("text") ?: "")
      "fraction" -> {
        val numString = map.getString("numerator") ?: ""
        val denString = map.getString("denominator") ?: ""
        val numRuns = if (map.hasKey("numeratorRuns")) {
          parseArray(map.getArray("numeratorRuns"))
        } else {
          listOf(RunNode.Text(numString))
        }
        val denRuns = if (map.hasKey("denominatorRuns")) {
          parseArray(map.getArray("denominatorRuns"))
        } else {
          listOf(RunNode.Text(denString))
        }
        RunNode.Fraction(numRuns, denRuns, numString, denString)
      }
      "superscript" -> RunNode.Superscript(parseArray(map.getArray("content")))
      "subscript" -> RunNode.Subscript(parseArray(map.getArray("content")))
      else -> null
    }
  }
}

/**
 * Recursive measurement + drawing for the library's run tree. Both
 * {@link FractionSpan} and {@link ScriptReplacementSpan} defer their
 * internal layout to this renderer so nested fractions (e.g. a stacked
 * exponent inside a fraction's denominator) share the same geometry.
 *
 * Measurements are returned in host pixels. The caller owns font weight
 * resolution and line-height integration.
 */
internal object FractionRenderer {

  /**
   * Compact, immutable layout result for a single {@link RunNode}. The
   * renderer measures each run top-down so the caller can align children
   * on a shared baseline.
   */
  data class Metrics(
    val width: Float,
    val ascent: Float,
    val descent: Float,
  ) {
    val height: Float get() = ascent + descent
  }

  /**
   * Measures a horizontal sequence of runs at the given host font size.
   * Width is the sum of individual widths; ascent / descent are the max
   * across runs so all pieces share one baseline.
   */
  fun measureRuns(
    runs: List<RunNode>,
    hostTextSizePx: Float,
    typeface: Typeface?,
    fontWeight: Int,
    barThicknessPxOverride: Float?,
  ): Metrics {
    if (runs.isEmpty()) return Metrics(0f, 0f, 0f)
    var width = 0f
    var ascent = 0f
    var descent = 0f
    for (run in runs) {
      val m = measureRun(
        run,
        hostTextSizePx,
        typeface,
        fontWeight,
        barThicknessPxOverride,
      )
      width += m.width
      ascent = max(ascent, m.ascent)
      descent = max(descent, m.descent)
    }
    return Metrics(width, ascent, descent)
  }

  fun measureRun(
    run: RunNode,
    hostTextSizePx: Float,
    typeface: Typeface?,
    fontWeight: Int,
    barThicknessPxOverride: Float?,
  ): Metrics {
    return when (run) {
      is RunNode.Text -> {
        val paint = makePaint(hostTextSizePx, typeface, fontWeight)
        val fm = paint.fontMetrics
        Metrics(
          width = paint.measureText(run.text),
          ascent = -fm.ascent,
          descent = fm.descent,
        )
      }
      is RunNode.Fraction -> measureFraction(
        run,
        hostTextSizePx,
        typeface,
        fontWeight,
        barThicknessPxOverride,
      )
      is RunNode.Superscript -> measureScript(
        run.content,
        hostTextSizePx,
        typeface,
        fontWeight,
        barThicknessPxOverride,
        raise = true,
      )
      is RunNode.Subscript -> measureScript(
        run.content,
        hostTextSizePx,
        typeface,
        fontWeight,
        barThicknessPxOverride,
        raise = false,
      )
    }
  }

  /**
   * Draws the given runs onto `canvas` starting at pen position `(x, baselineY)`.
   * Returns the total advance consumed so nested callers can chain.
   */
  fun drawRuns(
    canvas: Canvas,
    runs: List<RunNode>,
    startX: Float,
    baselineY: Float,
    hostTextSizePx: Float,
    typeface: Typeface?,
    fontWeight: Int,
    textColor: Int,
    barThicknessPxOverride: Float?,
  ): Float {
    var x = startX
    for (run in runs) {
      val advance = drawRun(
        canvas,
        run,
        x,
        baselineY,
        hostTextSizePx,
        typeface,
        fontWeight,
        textColor,
        barThicknessPxOverride,
      )
      x += advance
    }
    return x - startX
  }

  fun drawRun(
    canvas: Canvas,
    run: RunNode,
    x: Float,
    baselineY: Float,
    hostTextSizePx: Float,
    typeface: Typeface?,
    fontWeight: Int,
    textColor: Int,
    barThicknessPxOverride: Float?,
  ): Float {
    return when (run) {
      is RunNode.Text -> {
        val paint = makePaint(hostTextSizePx, typeface, fontWeight)
        paint.color = textColor
        canvas.drawText(run.text, x, baselineY, paint)
        paint.measureText(run.text)
      }
      is RunNode.Fraction -> drawFraction(
        canvas,
        run,
        x,
        baselineY,
        hostTextSizePx,
        typeface,
        fontWeight,
        textColor,
        barThicknessPxOverride,
      )
      is RunNode.Superscript -> drawScript(
        canvas,
        run.content,
        x,
        baselineY,
        hostTextSizePx,
        typeface,
        fontWeight,
        textColor,
        barThicknessPxOverride,
        raise = true,
      )
      is RunNode.Subscript -> drawScript(
        canvas,
        run.content,
        x,
        baselineY,
        hostTextSizePx,
        typeface,
        fontWeight,
        textColor,
        barThicknessPxOverride,
        raise = false,
      )
    }
  }

  // region Fraction layout ---------------------------------------------------

  /**
   * Layout geometry for a stacked fraction. Kept as a data class so
   * `measure` and `draw` stay in sync — any change here applies to both.
   */
  private data class FractionLayout(
    val fracTextSizePx: Float,
    val numMetrics: Metrics,
    val denMetrics: Metrics,
    val cellWidth: Float,
    val totalWidth: Float,
    val sidePadding: Float,
    val gap: Float,
    val barWidth: Float,
  )

  private fun layoutFraction(
    run: RunNode.Fraction,
    hostTextSizePx: Float,
    typeface: Typeface?,
    fontWeight: Int,
    barThicknessPxOverride: Float?,
  ): FractionLayout {
    val fracTextSizePx = hostTextSizePx * 0.75f
    val numMetrics = measureRuns(
      run.numerator,
      fracTextSizePx,
      typeface,
      fontWeight,
      barThicknessPxOverride,
    )
    val denMetrics = measureRuns(
      run.denominator,
      fracTextSizePx,
      typeface,
      fontWeight,
      barThicknessPxOverride,
    )
    val cell = max(numMetrics.width, denMetrics.width)
    val sidePadding = fracTextSizePx * 0.2f
    val gap = max(1f, fracTextSizePx * 0.08f)
    val barWidth = barThicknessPxOverride?.coerceAtLeast(0.5f)
      ?: max(1f, fracTextSizePx * 0.06f)
    return FractionLayout(
      fracTextSizePx = fracTextSizePx,
      numMetrics = numMetrics,
      denMetrics = denMetrics,
      cellWidth = cell,
      totalWidth = cell + sidePadding * 2,
      sidePadding = sidePadding,
      gap = gap,
      barWidth = barWidth,
    )
  }

  private fun measureFraction(
    run: RunNode.Fraction,
    hostTextSizePx: Float,
    typeface: Typeface?,
    fontWeight: Int,
    barThicknessPxOverride: Float?,
  ): Metrics {
    val l = layoutFraction(
      run,
      hostTextSizePx,
      typeface,
      fontWeight,
      barThicknessPxOverride,
    )
    // Center the fraction vertically on the host x-height.
    val hostPaint = makePaint(hostTextSizePx, typeface, fontWeight)
    val xHeightApprox = hostPaint.textSize * 0.5f
    val halfStack = l.numMetrics.height + l.gap + l.barWidth / 2f
    val ascent = halfStack + xHeightApprox / 2f
    val descent = (l.denMetrics.height + l.gap + l.barWidth / 2f) - xHeightApprox / 2f
    return Metrics(l.totalWidth, ascent, max(0f, descent))
  }

  private fun drawFraction(
    canvas: Canvas,
    run: RunNode.Fraction,
    x: Float,
    baselineY: Float,
    hostTextSizePx: Float,
    typeface: Typeface?,
    fontWeight: Int,
    textColor: Int,
    barThicknessPxOverride: Float?,
  ): Float {
    val l = layoutFraction(
      run,
      hostTextSizePx,
      typeface,
      fontWeight,
      barThicknessPxOverride,
    )
    val hostPaint = makePaint(hostTextSizePx, typeface, fontWeight)
    val xHeightApprox = hostPaint.textSize * 0.5f
    val barY = baselineY - xHeightApprox / 2f

    val numBaseline = barY - l.gap - l.numMetrics.descent
    val numX = x + l.sidePadding + (l.cellWidth - l.numMetrics.width) / 2f
    drawRuns(
      canvas,
      run.numerator,
      numX,
      numBaseline,
      l.fracTextSizePx,
      typeface,
      fontWeight,
      textColor,
      barThicknessPxOverride,
    )

    val barPaint = Paint().apply {
      isAntiAlias = true
      color = textColor
      strokeWidth = l.barWidth
      style = Paint.Style.STROKE
    }
    val barStart = x + l.sidePadding * 0.5f
    val barEnd = x + l.sidePadding * 1.5f + l.cellWidth
    canvas.drawLine(barStart, barY, barEnd, barY, barPaint)

    val denBaseline = barY + l.gap + l.denMetrics.ascent
    val denX = x + l.sidePadding + (l.cellWidth - l.denMetrics.width) / 2f
    drawRuns(
      canvas,
      run.denominator,
      denX,
      denBaseline,
      l.fracTextSizePx,
      typeface,
      fontWeight,
      textColor,
      barThicknessPxOverride,
    )

    return l.totalWidth
  }

  // endregion

  // region Script layout -----------------------------------------------------

  /**
   * Script offset: raised baselines land `capHeight * 0.45` above the host
   * baseline; lowered drop the same amount below. Uses `textSize * 0.7` as
   * a proxy for capHeight since `Paint.FontMetrics.top` isn't granular
   * enough across OEM fonts.
   */
  private fun scriptBaselineOffset(hostTextSizePx: Float): Float =
    hostTextSizePx * 0.7f * 0.45f

  private fun measureScript(
    content: List<RunNode>,
    hostTextSizePx: Float,
    typeface: Typeface?,
    fontWeight: Int,
    barThicknessPxOverride: Float?,
    raise: Boolean,
  ): Metrics {
    val scriptTextSizePx = hostTextSizePx * 0.65f
    val inner = measureRuns(
      content,
      scriptTextSizePx,
      typeface,
      fontWeight,
      barThicknessPxOverride,
    )
    val offset = scriptBaselineOffset(hostTextSizePx)
    return if (raise) {
      Metrics(inner.width, inner.ascent + offset, max(0f, inner.descent - offset))
    } else {
      Metrics(inner.width, max(0f, inner.ascent - offset), inner.descent + offset)
    }
  }

  private fun drawScript(
    canvas: Canvas,
    content: List<RunNode>,
    x: Float,
    baselineY: Float,
    hostTextSizePx: Float,
    typeface: Typeface?,
    fontWeight: Int,
    textColor: Int,
    barThicknessPxOverride: Float?,
    raise: Boolean,
  ): Float {
    val scriptTextSizePx = hostTextSizePx * 0.65f
    val offset = scriptBaselineOffset(hostTextSizePx)
    val shiftedBaseline = if (raise) baselineY - offset else baselineY + offset
    return drawRuns(
      canvas,
      content,
      x,
      shiftedBaseline,
      scriptTextSizePx,
      typeface,
      fontWeight,
      textColor,
      barThicknessPxOverride,
    )
  }

  // endregion

  private fun makePaint(
    textSizePx: Float,
    typeface: Typeface?,
    fontWeight: Int,
  ): TextPaint {
    val paint = TextPaint().apply {
      isAntiAlias = true
      textSize = textSizePx
    }
    if (typeface != null) {
      paint.typeface = if (
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P
      ) {
        Typeface.create(typeface, fontWeight, false)
      } else {
        typeface
      }
    }
    return paint
  }
}
