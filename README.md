# react-native-fractions

Native **stacked fractions** for React Native: numerator over denominator with a horizontal rule, laid out by the platform text engine so fractions wrap and align with surrounding text like normal characters.

**Tokenization is your responsibility.** This library only renders an array of **runs** (`text` segments and `fraction` segments with `numerator` / `denominator` strings). Build those runs however you like (regex, parser, CMS).

## Requirements

- React Native **0.73+** (developed against 0.82)
- React **18+**
- iOS **13.4+**
- Android **minSdk** as required by your app (library targets API 24+)

## Installation

```bash
yarn add react-native-fractions
# or
npm install react-native-fractions
```

### iOS

```bash
cd ios && pod install
```

Autolinking registers the native module; no manual `AppDelegate` changes.

### Android

Autolinking registers `ReactNativeFractionsPackage`. No manual `MainApplication` edits on modern React Native.

## Usage

```tsx
import { FractionText, type TokenRun } from 'react-native-fractions';

const runs: TokenRun[] = [
  { type: 'text', text: 'The answer is ' },
  { type: 'fraction', numerator: '3', denominator: '4' },
  { type: 'text', text: ' metres.' },
];

export function Example() {
  return (
    <FractionText
      runs={runs}
      fontSize={16}
      lineHeight={22}
      color="#111111"
      fontFamily="YourFont-Regular"
      fontWeight="400"
      textAlign="left"
    />
  );
}
```

If your app already tokenizes strings (e.g. `a/b` patterns), pass the resulting `TokenRun[]` straight through.

## API

### `FractionText`

| Prop | Type | Description |
|------|------|-------------|
| `runs` | `TokenRun[]` | Ordered text and fraction segments (serializable for the RN bridge). |
| `fontSize` | `number` | Size in dp/pt (same as `Text`). |
| `lineHeight` | `number` | Minimum line height for layout. |
| `color` | `string` | e.g. `#000`, `rgba(...)`. |
| `fontFamily` | `string` | Optional registered font family name. |
| `fontWeight` | `string` | Optional, e.g. `400`, `600`, `700`. |
| `textAlign` | `'left' \| 'center' \| 'right'` | Optional. |
| `style` | `ViewStyle` | Optional wrapper styles. |

### Run types

- **`{ type: 'text', text: string }`** — plain text.
- **`{ type: 'fraction', numerator: string, denominator: string }`** — stacked fraction; strings can include signs (e.g. `-b`).

### Native view name

The underlying native component is registered as **`FractionText`** (`requireNativeComponent('FractionText')`). If the native module is missing (e.g. forgot `pod install` / rebuild), the JS layer falls back to a plain `Text` stub joining fractions as `numerator/denominator` so the issue is visible in development.

## How it works

- **iOS:** `NSTextAttachment` + attributed string (see `ios/`).
- **Android:** `SpannableStringBuilder` with `ReplacementSpan` for the fraction glyph (see `android/`).

## Contributing

Clone the repo, install dependencies, run tests:

```bash
npm install
npm run typecheck
npm test
```

Link into an app for integration checks:

```bash
npm install file:../path/to/react-native-fractions
```

When developing locally against a consuming app, add the package root to Metro's `watchFolders` and `resolver.extraNodeModules` so edits in `src/` hot-reload (see the `metro.config.js` example in this repo's parent project).

## Build

The published artifact is produced by [`react-native-builder-bob`](https://github.com/callstack/react-native-builder-bob):

```bash
npm run clean
npm run build
```

This emits:

- `lib/commonjs/` — CommonJS for Node/Jest consumers
- `lib/module/` — ESM for bundlers
- `lib/typescript/` — `.d.ts` declarations

## Release

1. Bump `version` in `package.json` (follow semver).
2. Update `CHANGELOG.md` if present.
3. Commit and tag: `git tag v$(node -p "require('./package.json').version") && git push --follow-tags`.
4. Publish:

   ```bash
   npm publish
   ```

   `prepublishOnly` runs `clean`, `typecheck`, `test:ci`, and `build` automatically, so a broken build or failing test blocks the release.

The first publish must be done by a maintainer with access to the `react-native-fractions` npm package under `@maderesponsively`-affiliated credentials; subsequent releases can be automated via GitHub Actions if desired.

## License

MIT — see [LICENSE](./LICENSE). Copyright (c) 2026 Made Responsively Ltd.
