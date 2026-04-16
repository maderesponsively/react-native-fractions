import React from 'react';
import TestRenderer, { act } from 'react-test-renderer';

/** Set by the hoisted `jest.mock('react-native')` factory (must be `var` for Jest hoisting). */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
var mockNativeView: jest.Mock;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
var mockGetViewManagerConfig: jest.Mock;

jest.mock('react-native', () => {
  mockNativeView = jest.fn(function MockFractionText() {
    return null;
  });
  mockGetViewManagerConfig = jest.fn(() => ({}));
  return {
    Platform: { OS: 'ios' },
    StyleSheet: {
      create: (styles: Record<string, unknown>) => styles,
    },
    Text: function MockText(props: { children?: React.ReactNode }) {
      return props.children ?? null;
    },
    UIManager: {
      getViewManagerConfig: mockGetViewManagerConfig,
    },
    requireNativeComponent: jest.fn(() => mockNativeView),
  };
});

// eslint-disable-next-line @typescript-eslint/no-var-requires
const { FractionText } = require('../src/FractionText') as typeof import('../src/FractionText');

describe('FractionText (native registered)', () => {
  beforeEach(() => {
    mockNativeView.mockClear();
  });

  it('forwards runs and typography props to the native component', () => {
    const runs = [
      { type: 'text' as const, text: 'x = ' },
      { type: 'fraction' as const, numerator: '1', denominator: '2' },
    ];

    act(() => {
      TestRenderer.create(
        <FractionText
          runs={runs}
          fontSize={16}
          lineHeight={22}
          color="#111111"
          fontFamily="System"
          fontWeight="400"
          textAlign="left"
        />,
      );
    });

    expect(mockNativeView).toHaveBeenCalled();
    const props = mockNativeView.mock.calls[0][0];
    expect(props.runs).toEqual(runs);
    expect(props.fontSize).toBe(16);
    expect(props.lineHeight).toBe(22);
    expect(props.color).toBe('#111111');
    expect(props.fontFamily).toBe('System');
    expect(props.fontWeight).toBe('400');
    expect(props.textAlign).toBe('left');
    expect(props.onContentSizeChange).toBeDefined();
  });

  it('forwards barThickness to the native component when provided', () => {
    const runs = [
      { type: 'fraction' as const, numerator: '1', denominator: '2' },
    ];

    act(() => {
      TestRenderer.create(
        <FractionText
          runs={runs}
          fontSize={16}
          lineHeight={22}
          color="#000"
          barThickness={2.5}
        />,
      );
    });

    const props = mockNativeView.mock.calls.at(-1)?.[0];
    expect(props.barThickness).toBe(2.5);
  });

  it('omits barThickness from the native component when not provided', () => {
    const runs = [
      { type: 'fraction' as const, numerator: '1', denominator: '2' },
    ];

    act(() => {
      TestRenderer.create(
        <FractionText runs={runs} fontSize={16} lineHeight={22} color="#000" />,
      );
    });

    const props = mockNativeView.mock.calls.at(-1)?.[0];
    expect(props.barThickness).toBeUndefined();
  });

  it('debounces content-size updates within 0.5dp', () => {
    const runs = [{ type: 'text' as const, text: 'abc' }];
    let renderer!: TestRenderer.ReactTestRenderer;

    act(() => {
      renderer = TestRenderer.create(
        <FractionText runs={runs} fontSize={16} lineHeight={22} color="#000" />,
      );
    });

    const firstProps = mockNativeView.mock.calls[0][0];
    const heightsSeen: unknown[] = [firstProps.style];

    act(() => {
      firstProps.onContentSizeChange({ nativeEvent: { width: 100, height: 22 } });
    });
    heightsSeen.push(mockNativeView.mock.calls.at(-1)?.[0].style);

    act(() => {
      mockNativeView.mock.calls
        .at(-1)?.[0]
        .onContentSizeChange({ nativeEvent: { width: 100, height: 22.3 } });
    });
    heightsSeen.push(mockNativeView.mock.calls.at(-1)?.[0].style);

    act(() => {
      mockNativeView.mock.calls
        .at(-1)?.[0]
        .onContentSizeChange({ nativeEvent: { width: 100, height: 30 } });
    });

    const flattenStyle = (s: unknown): Record<string, unknown> => {
      const arr = Array.isArray(s) ? s : [s];
      return Object.assign({}, ...arr.filter(Boolean));
    };

    const lastStyle = flattenStyle(mockNativeView.mock.calls.at(-1)?.[0].style);
    expect(lastStyle.height).toBe(30);

    act(() => {
      renderer.unmount();
    });
  });
});
