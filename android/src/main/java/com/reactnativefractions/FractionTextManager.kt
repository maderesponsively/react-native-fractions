package com.reactnativefractions

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

/**
 * View manager exposing `FractionText` to JavaScript (`requireNativeComponent`).
 */
class FractionTextManager : SimpleViewManager<FractionTextView>() {

  override fun getName(): String = REACT_CLASS

  override fun createViewInstance(reactContext: ThemedReactContext): FractionTextView {
    return FractionTextView(reactContext)
  }

  @ReactProp(name = "runs")
  fun setRuns(view: FractionTextView, value: ReadableArray?) {
    view.setRuns(value)
  }

  @ReactProp(name = "fontSize", defaultDouble = 14.0)
  fun setFontSize(view: FractionTextView, value: Double) {
    view.setFontSize(value)
  }

  @ReactProp(name = "lineHeight", defaultDouble = 20.0)
  fun setLineHeight(view: FractionTextView, value: Double) {
    view.setLineHeightDp(value)
  }

  @ReactProp(name = "color", customType = "Color")
  fun setColor(view: FractionTextView, value: Int?) {
    view.setTextColorInt(value ?: 0xFF000000.toInt())
  }

  @ReactProp(name = "fontFamily")
  fun setFontFamily(view: FractionTextView, value: String?) {
    view.setFontFamilyName(value)
  }

  @ReactProp(name = "fontWeight")
  fun setFontWeight(view: FractionTextView, value: String?) {
    view.setFontWeightStr(value)
  }

  @ReactProp(name = "textAlign")
  fun setTextAlign(view: FractionTextView, value: String?) {
    view.setTextAlignStr(value)
  }

  @ReactProp(name = "barThickness")
  fun setBarThickness(view: FractionTextView, value: Double) {
    view.setBarThicknessDp(if (value > 0.0) value else null)
  }

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
    return mutableMapOf(
      "topContentSizeChange" to mutableMapOf<String, Any>(
        "registrationName" to "onContentSizeChange",
      ),
    )
  }

  companion object {
    const val REACT_CLASS = "FractionText"
  }
}
