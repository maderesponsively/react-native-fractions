import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  NativeSyntheticEvent,
  Platform,
  StyleProp,
  StyleSheet,
  Text as RNText,
  UIManager,
  ViewStyle,
  requireNativeComponent,
} from 'react-native';

import type { TokenRun } from './types';

/**
 * Native view name registered on iOS (`FractionTextManager`) and Android
 * (`FractionTextManager`).
 */
const NATIVE_COMPONENT_NAME = 'FractionText';

type ContentSizeEvent = NativeSyntheticEvent<{
  width: number;
  height: number;
}>;

interface NativeViewProps {
  runs: TokenRun[];
  fontSize: number;
  lineHeight: number;
  color: string;
  fontFamily?: string;
  fontWeight?: string;
  textAlign?: 'left' | 'center' | 'right';
  barThickness?: number;
  onContentSizeChange?: (event: ContentSizeEvent) => void;
  style?: StyleProp<ViewStyle>;
}

const isNativeRegistered = (() => {
  if (Platform.OS !== 'ios' && Platform.OS !== 'android') return false;
  const config =
    (UIManager as unknown as { getViewManagerConfig?: (n: string) => unknown })
      .getViewManagerConfig?.(NATIVE_COMPONENT_NAME) ??
    (UIManager as unknown as Record<string, unknown>)[NATIVE_COMPONENT_NAME];
  return config != null;
})();

const NativeView = isNativeRegistered
  ? requireNativeComponent<NativeViewProps>(NATIVE_COMPONENT_NAME)
  : null;

declare const __DEV__: boolean | undefined;

/**
 * Props for {@link FractionText}. **Tokenization** (what becomes numerator vs
 * denominator) is owned by the app; pass pre-built {@link TokenRun} arrays.
 */
export interface FractionTextProps {
  /**
   * Ordered list of text and fraction runs. Typically produced by your own
   * tokenizer (e.g. regex over `a/b` patterns).
   */
  runs: TokenRun[];
  /** Font size in logical pixels (same sense as React Native `Text`). */
  fontSize: number;
  /** Line height in dp/pt; native layout uses this for line metrics. */
  lineHeight: number;
  /**
   * Color string (e.g. `"#000000"`, `"rgba(...)"`). Converted to native color
   * by React Native like `<Text>`.
   */
  color: string;
  /** Registered custom font family name, or system default if omitted. */
  fontFamily?: string;
  /** Weight string, e.g. `"400"`, `"600"`, `"700"`. */
  fontWeight?: string;
  textAlign?: 'left' | 'center' | 'right';
  /**
   * Stroke thickness of the fraction rule (vinculum) in dp/pt. When omitted,
   * the native side draws a rule at approximately 6% of the fraction font
   * size, which is a good default for body text. Provide an absolute value
   * (e.g. `2`) for a heavier rule or a thinner one (e.g. `0.5`).
   */
  barThickness?: number;
  style?: StyleProp<ViewStyle>;
}

/**
 * Renders mixed text and stacked fractions using the **native** text engine
 * (`NSTextAttachment` on iOS, `ReplacementSpan` on Android) so fractions
 * participate in line breaking and baseline alignment like ordinary text.
 *
 * If the native view is not linked, falls back to a plain {@link RNText} stub
 * that joins fraction runs as `numerator/denominator` so missing native
 * builds are obvious during development.
 */
export const FractionText: React.FC<FractionTextProps> = ({
  runs,
  fontSize,
  lineHeight,
  color,
  fontFamily,
  fontWeight,
  textAlign,
  barThickness,
  style,
}) => {
  const [contentHeight, setContentHeight] = useState<number | null>(null);
  const warnedRef = useRef(false);

  const handleContentSize = useCallback((event: ContentSizeEvent) => {
    const { height } = event.nativeEvent;
    if (typeof height === 'number' && height > 0) {
      setContentHeight(prev =>
        prev == null || Math.abs(prev - height) > 0.5 ? height : prev,
      );
    }
  }, []);

  useEffect(() => {
    if (
      !NativeView &&
      !warnedRef.current &&
      typeof __DEV__ !== 'undefined' &&
      __DEV__
    ) {
      warnedRef.current = true;
      console.warn(
        '[react-native-fractions] native view "FractionText" is not registered — rebuild the native app after installing this library.',
      );
    }
  }, []);

  if (!NativeView) {
    const flattened = runs
      .map(run =>
        run.type === 'text' ? run.text : `${run.numerator}/${run.denominator}`,
      )
      .join('');
    return (
      <RNText
        style={[
          {
            fontSize,
            lineHeight,
            color,
            fontFamily,
            fontWeight: fontWeight as never,
            textAlign,
          },
          style,
        ]}
      >
        {flattened}
      </RNText>
    );
  }

  return (
    <NativeView
      runs={runs}
      fontSize={fontSize}
      lineHeight={lineHeight}
      color={color}
      fontFamily={fontFamily}
      fontWeight={fontWeight}
      textAlign={textAlign}
      barThickness={barThickness}
      onContentSizeChange={handleContentSize}
      style={[
        styles.native,
        { minHeight: lineHeight },
        contentHeight != null ? { height: contentHeight } : null,
        style,
      ]}
    />
  );
};

const styles = StyleSheet.create({
  native: {
    alignSelf: 'stretch',
  },
});
