/**
 * A plain text segment. Serialized to the native `FractionText` view as a
 * bridge-friendly object (see {@link TokenRun}).
 */
export type TextRun = {
  type: 'text';
  /** Literal characters to render with the surrounding typography. */
  text: string;
};

/**
 * A stacked fraction (numerator over denominator). The native layer draws a
 * vinculum; **your app** decides how strings are split (e.g. regex
 * tokenization).
 *
 * @remarks
 * The string `numerator` / `denominator` fields remain the canonical
 * representation so this run shape is still understood by library versions
 * `< 0.3.0`. The optional `numeratorRuns` / `denominatorRuns` arrays let
 * richer consumers nest additional runs — including further fractions and
 * {@link SuperscriptRun} / {@link SubscriptRun} — on either side of the
 * vinculum. When a structured array is present the native renderer uses it
 * verbatim; otherwise it falls back to the string field.
 */
export type FractionRun = {
  type: 'fraction';
  numerator: string;
  denominator: string;
  /**
   * Optional structured numerator; overrides `numerator` when present.
   * Each nested `FractionRun` renders at ~75% of the parent font size,
   * allowing fractions-inside-fractions without manual font scaling.
   */
  numeratorRuns?: TokenRun[];
  /**
   * Optional structured denominator; overrides `denominator` when present.
   * Same semantics as {@link FractionRun.numeratorRuns}. Commonly used
   * together with a {@link SuperscriptRun} to render a base + exponent
   * ("stacked exponent") as the denominator of an outer fraction.
   */
  denominatorRuns?: TokenRun[];
};

/**
 * Raised-baseline content drawn at ~65% of the parent font size. Content
 * may be any mix of {@link TokenRun}, including a {@link FractionRun} —
 * producing a shrunk stacked fraction at superscript height (e.g. the
 * `-3/2` in `25^(-3/2)`).
 *
 * @remarks
 * Added in library version 0.3.0. Older versions silently drop this run
 * type, so app-side tokenizers that target mixed-version audiences should
 * still emit a plain-text Unicode-superscript fallback where the content
 * is representable that way.
 */
export type SuperscriptRun = {
  type: 'superscript';
  /** Runs to render at superscript position, rendered at 0.65x scale. */
  content: TokenRun[];
};

/**
 * Lowered-baseline counterpart of {@link SuperscriptRun}. Same scale
 * (~0.65x) with the baseline dropped to subscript position. Included for
 * symmetry; requires library version 0.3.0+.
 */
export type SubscriptRun = {
  type: 'subscript';
  /** Runs to render at subscript position, rendered at 0.65x scale. */
  content: TokenRun[];
};

/**
 * Union of serializable runs accepted by `FractionText`. Must remain
 * JSON-serializable (no functions or React nodes) so it can cross the RN
 * bridge.
 *
 * @remarks
 * Library version 0.3.0 added {@link SuperscriptRun} and
 * {@link SubscriptRun}, plus the optional `numeratorRuns` /
 * `denominatorRuns` fields on {@link FractionRun}. All additions are
 * non-breaking: existing consumers that only emit `TextRun` and
 * string-based `FractionRun` continue to render identically.
 */
export type TokenRun = TextRun | FractionRun | SuperscriptRun | SubscriptRun;
