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
 * tokenization). Only `numerator` and `denominator` strings are sent native.
 */
export type FractionRun = {
  type: 'fraction';
  numerator: string;
  denominator: string;
};

/**
 * Union of serializable runs accepted by {@link FractionText}. Must remain
 * JSON-serializable (no functions or React nodes) so it can cross the RN bridge.
 */
export type TokenRun = TextRun | FractionRun;
