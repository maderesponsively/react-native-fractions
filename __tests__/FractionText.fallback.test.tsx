import React from 'react';
import TestRenderer, { act } from 'react-test-renderer';

jest.mock('react-native', () => {
  return {
    Platform: { OS: 'ios' },
    StyleSheet: {
      create: (styles: Record<string, unknown>) => styles,
    },
    Text: function MockText(props: { children?: React.ReactNode }) {
      return props.children ?? null;
    },
    UIManager: {
      getViewManagerConfig: jest.fn(() => null),
    },
    requireNativeComponent: jest.fn(() => () => null),
  };
});

(global as unknown as { __DEV__: boolean }).__DEV__ = false;

// eslint-disable-next-line @typescript-eslint/no-var-requires
const { FractionText } = require('../src/FractionText') as typeof import('../src/FractionText');

describe('FractionText (native missing fallback)', () => {
  it('renders joined numerator/denominator as plain text when native view is absent', () => {
    const runs = [
      { type: 'text' as const, text: 'x = ' },
      { type: 'fraction' as const, numerator: '1', denominator: '2' },
      { type: 'text' as const, text: ' done' },
    ];

    let tree!: TestRenderer.ReactTestRenderer;
    act(() => {
      tree = TestRenderer.create(
        <FractionText runs={runs} fontSize={16} lineHeight={22} color="#000" />,
      );
    });

    const json = tree.toJSON();
    expect(JSON.stringify(json)).toContain('x = 1/2 done');
  });

  it('does not warn in production (__DEV__=false)', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    act(() => {
      TestRenderer.create(
        <FractionText
          runs={[{ type: 'text', text: 'hi' }]}
          fontSize={16}
          lineHeight={22}
          color="#000"
        />,
      );
    });
    expect(warnSpy).not.toHaveBeenCalled();
    warnSpy.mockRestore();
  });
});
