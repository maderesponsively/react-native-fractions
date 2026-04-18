jest.mock('react-native', () => ({
  Platform: { OS: 'ios' },
  StyleSheet: { create: (s: Record<string, unknown>) => s },
  Text: function Text() {
    return null;
  },
  UIManager: { getViewManagerConfig: () => ({}) },
  requireNativeComponent: () => function Native() {
    return null;
  },
}));

// eslint-disable-next-line @typescript-eslint/no-var-requires
const pkg = require('../src/index');

describe('package entry', () => {
  it('exports FractionText as a component', () => {
    expect(typeof pkg.FractionText).toBe('function');
  });

  it('type-only exports do not leak runtime values', () => {
    expect(pkg.TextRun).toBeUndefined();
    expect(pkg.FractionRun).toBeUndefined();
    expect(pkg.SuperscriptRun).toBeUndefined();
    expect(pkg.SubscriptRun).toBeUndefined();
    expect(pkg.TokenRun).toBeUndefined();
  });
});
