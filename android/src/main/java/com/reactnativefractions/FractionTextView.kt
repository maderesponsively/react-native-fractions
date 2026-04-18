package com.reactnativefractions

import android.content.Context
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.common.assets.ReactFontManager
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.events.Event

/**
 * Native backing view for the RN `FractionText` component.
 */
class FractionTextView(context: Context) : AppCompatTextView(context) {

  private var runs: ReadableArray? = null
  private var lineHeightPx: Float = 0f
  private var fontFamilyName: String? = null
  private var fontWeightStr: String? = null
  private var resolvedFontWeight: Int = 400
  private var resolvedTypeface: Typeface? = null
  private var barThicknessDp: Double? = null
  private var dirty = true
  private var lastReportedWidthDp = -1.0
  private var lastReportedHeightDp = -1.0

  init {
    includeFontPadding = false
    setPadding(0, 0, 0, 0)
    setSingleLine(false)
    maxLines = Int.MAX_VALUE
  }

  fun setRuns(value: ReadableArray?) {
    runs = value
    dirty = true
    requestRebuild()
  }

  fun setFontSize(sizeSp: Double) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp.toFloat())
    dirty = true
    requestRebuild()
  }

  fun setLineHeightDp(valueDp: Double) {
    val density = resources.displayMetrics.density
    lineHeightPx = (valueDp * density).toFloat()
    applyLineHeight()
    dirty = true
    requestRebuild()
  }

  fun setTextColorInt(value: Int) {
    setTextColor(value)
    dirty = true
    requestRebuild()
  }

  fun setFontFamilyName(name: String?) {
    fontFamilyName = name
    applyTypeface()
    dirty = true
    requestRebuild()
  }

  fun setFontWeightStr(weight: String?) {
    fontWeightStr = weight
    applyTypeface()
    dirty = true
    requestRebuild()
  }

  fun setBarThicknessDp(value: Double?) {
    barThicknessDp = value
    dirty = true
    requestRebuild()
  }

  fun setTextAlignStr(value: String?) {
    textAlignment = when (value) {
      "center" -> TEXT_ALIGNMENT_CENTER
      "right" -> TEXT_ALIGNMENT_VIEW_END
      "left" -> TEXT_ALIGNMENT_VIEW_START
      else -> TEXT_ALIGNMENT_GRAVITY
    }
    gravity = when (value) {
      "center" -> android.view.Gravity.CENTER_HORIZONTAL
      "right" -> android.view.Gravity.END
      "left" -> android.view.Gravity.START
      else -> android.view.Gravity.START
    }
  }

  private fun applyTypeface() {
    val weight = mapWeight(fontWeightStr)
    resolvedFontWeight = weight
    val typefaceValue = resolveTypeface(fontFamilyName, weight)
    resolvedTypeface = typefaceValue
    if (BuildConfig.DEBUG) {
      Log.d(
        TAG,
        "applyTypeface family=$fontFamilyName weight=$weight resolved=$typefaceValue",
      )
    }
    setTypeface(typefaceValue)
    paint.typeface = typefaceValue
  }

  private fun resolveTypeface(family: String?, weight: Int): Typeface {
    if (family.isNullOrEmpty()) {
      return Typeface.defaultFromStyle(styleFromWeight(weight))
    }
    val viaRn = runCatching {
      ReactFontManager.getInstance()
        .getTypeface(family, weight, false, context.assets)
    }.getOrNull()
    if (viaRn != null && viaRn != Typeface.DEFAULT) {
      return viaRn
    }
    if (BuildConfig.DEBUG) {
      Log.d(
        TAG,
        "resolveTypeface: ReactFontManager returned default for $family, " +
          "falling back to direct asset load",
      )
    }
    val direct = runCatching {
      Typeface.createFromAsset(context.assets, "fonts/$family.ttf")
    }.getOrNull()
    if (direct != null) return direct
    if (BuildConfig.DEBUG) {
      Log.w(
        TAG,
        "resolveTypeface: could not load fonts/$family.ttf, using system default",
      )
    }
    return Typeface.defaultFromStyle(styleFromWeight(weight))
  }

  private fun mapWeight(raw: String?): Int = when (raw) {
    "100" -> 100
    "200" -> 200
    "300" -> 300
    "400", "normal", null, "" -> 400
    "500" -> 500
    "600" -> 600
    "700", "bold" -> 700
    "800" -> 800
    "900" -> 900
    else -> raw.toIntOrNull()?.coerceIn(100, 900) ?: 400
  }

  private fun styleFromWeight(weight: Int): Int =
    if (weight >= 700) Typeface.BOLD else Typeface.NORMAL

  private fun applyLineHeight() {
    if (lineHeightPx <= 0f) return
    val fm = paint.fontMetrics
    val textLineHeight = fm.descent - fm.ascent
    val extra = lineHeightPx - textLineHeight
    if (extra > 0f) {
      setLineSpacing(extra, 1f)
    } else {
      setLineSpacing(0f, 1f)
    }
  }

  private fun requestRebuild() {
    if (dirty) {
      rebuildText()
      dirty = false
      requestLayout()
    }
  }

  private fun effectiveTypeface(): Typeface {
    resolvedTypeface?.let { return it }
    val tf = resolveTypeface(fontFamilyName, resolvedFontWeight)
    resolvedTypeface = tf
    return tf
  }

  private fun rebuildText() {
    val currentRuns = runs ?: return
    val builder = SpannableStringBuilder()
    val tf = effectiveTypeface()
    if (BuildConfig.DEBUG) {
      Log.d(
        TAG,
        "rebuildText family=$fontFamilyName weight=$resolvedFontWeight " +
          "tf=$tf runs=${currentRuns.size()}",
      )
    }
    val density = resources.displayMetrics.density
    val barPxOverride: Float? = barThicknessDp
      ?.takeIf { it > 0.0 }
      ?.let { (it * density).toFloat() }
    for (i in 0 until currentRuns.size()) {
      val map: ReadableMap = currentRuns.getMap(i) ?: continue
      val type = map.getString("type") ?: continue
      when (type) {
        "text" -> {
          val s = map.getString("text")
          if (!s.isNullOrEmpty()) {
            val start = builder.length
            builder.append(s)
            builder.setSpan(FontSpan(tf), start, builder.length, 0)
          }
        }
        "fraction" -> {
          val node = RunNodeParser.parseMap(map) as? RunNode.Fraction ?: continue
          val start = builder.length
          builder.append("\uFFFC")
          val span = FractionSpan(
            numeratorRuns = node.numerator,
            denominatorRuns = node.denominator,
            hostTextSizePx = textSize,
            textColor = currentTextColor,
            typeface = tf,
            fontWeight = resolvedFontWeight,
            barThicknessPxOverride = barPxOverride,
          )
          builder.setSpan(span, start, start + 1, 0)
        }
        "superscript", "subscript" -> {
          val node = RunNodeParser.parseMap(map) ?: continue
          val content = when (node) {
            is RunNode.Superscript -> node.content
            is RunNode.Subscript -> node.content
            else -> continue
          }
          val start = builder.length
          builder.append("\uFFFC")
          val span = ScriptReplacementSpan(
            content = content,
            hostTextSizePx = textSize,
            textColor = currentTextColor,
            typeface = tf,
            fontWeight = resolvedFontWeight,
            barThicknessPxOverride = barPxOverride,
            raise = type == "superscript",
          )
          builder.setSpan(span, start, start + 1, 0)
        }
      }
    }
    setText(builder, BufferType.SPANNABLE)
  }

  override fun onLayout(
    changed: Boolean,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
  ) {
    super.onLayout(changed, left, top, right, bottom)
    reportContentSize()
  }

  private fun reportContentSize() {
    val layoutRef: Layout? = layout
    val measuredWidth = (layoutRef?.width ?: width).toFloat()
    val measuredHeight = (layoutRef?.height ?: height).toFloat()
    val density = resources.displayMetrics.density
    val widthDp = (measuredWidth / density).toDouble()
    val heightDp = (measuredHeight / density).toDouble()
    if (heightDp <= 0) return
    if (
      Math.abs(widthDp - lastReportedWidthDp) < 0.5 &&
      Math.abs(heightDp - lastReportedHeightDp) < 0.5
    ) return
    lastReportedWidthDp = widthDp
    lastReportedHeightDp = heightDp
    val ctx = context as? ReactContext ?: return
    val dispatcher = UIManagerHelper.getEventDispatcherForReactTag(ctx, id) ?: return
    val surfaceId = UIManagerHelper.getSurfaceId(this)
    dispatcher.dispatchEvent(
      ContentSizeChangeEvent(surfaceId, id, widthDp, heightDp),
    )
  }

  private class ContentSizeChangeEvent(
    surfaceId: Int,
    viewId: Int,
    private val widthDp: Double,
    private val heightDp: Double,
  ) : Event<ContentSizeChangeEvent>(surfaceId, viewId) {
    override fun getEventName(): String = "topContentSizeChange"
    override fun getEventData(): com.facebook.react.bridge.WritableMap {
      val map = Arguments.createMap()
      map.putDouble("width", widthDp)
      map.putDouble("height", heightDp)
      return map
    }
  }

  companion object {
    private const val TAG = "FractionText"

    @Suppress("unused")
    fun layoutParamsForWrap() = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
    )
  }
}
