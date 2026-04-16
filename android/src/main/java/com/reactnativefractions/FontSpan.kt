package com.reactnativefractions

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * Span that unconditionally applies the provided [typeface] to the text
 * paint for its covered range.
 */
class FontSpan(private val typeface: Typeface) : MetricAffectingSpan() {
  override fun updateDrawState(tp: TextPaint) {
    tp.typeface = typeface
  }

  override fun updateMeasureState(tp: TextPaint) {
    tp.typeface = typeface
  }
}
