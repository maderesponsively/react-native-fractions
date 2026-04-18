import UIKit

/**
 * Shared helpers for rendering nested run trees onto `UIImage`s that sit
 * inside `NSTextAttachment`s. Extracted so that fractions, scripts (super /
 * subscript) and nested-fraction numerator / denominator arrays can all
 * reuse the same drawing logic.
 */
enum FractionRenderer {
  /**
   * Renders an array of runs onto a single `UIImage`, laying them out
   * horizontally on a common baseline. Used to draw a fraction's numerator
   * or denominator when the app supplies a structured `numeratorRuns` /
   * `denominatorRuns` array (library schema 0.3.0+).
   */
  static func image(
    forRuns runs: [Any],
    font: UIFont,
    color: UIColor,
    barWidth: CGFloat?
  ) -> UIImage {
    let pieces = runs.compactMap { rawRun -> (image: UIImage, baseline: CGFloat)? in
      guard let dict = rawRun as? [String: Any],
            let type = dict["type"] as? String else { return nil }
      switch type {
      case "text":
        let text = (dict["text"] as? String) ?? ""
        return renderText(text, font: font, color: color)
      case "fraction":
        return renderFraction(
          dict,
          font: font,
          color: color,
          barWidth: barWidth
        )
      case "superscript":
        return renderScript(
          dict["content"] as? [Any] ?? [],
          font: font,
          color: color,
          barWidth: barWidth,
          raise: true
        )
      case "subscript":
        return renderScript(
          dict["content"] as? [Any] ?? [],
          font: font,
          color: color,
          barWidth: barWidth,
          raise: false
        )
      default:
        return nil
      }
    }

    if pieces.isEmpty {
      return UIImage()
    }

    // Align pieces on a shared baseline. `baseline` is the distance from
    // the top of each piece's image to its baseline; we pick the max so
    // ascent-heavy pieces (fractions) stay above the x-height line.
    let maxBaseline = pieces.map(\.baseline).max() ?? 0
    let maxBelow = pieces
      .map { $0.image.size.height - $0.baseline }
      .max() ?? 0
    let totalWidth = pieces.map(\.image.size.width).reduce(0, +)
    let totalHeight = maxBaseline + maxBelow

    let format = UIGraphicsImageRendererFormat.default()
    format.opaque = false
    let renderer = UIGraphicsImageRenderer(
      size: CGSize(width: totalWidth, height: totalHeight),
      format: format
    )
    return renderer.image { _ in
      var x: CGFloat = 0
      for piece in pieces {
        let y = maxBaseline - piece.baseline
        piece.image.draw(
          in: CGRect(
            x: x,
            y: y,
            width: piece.image.size.width,
            height: piece.image.size.height
          )
        )
        x += piece.image.size.width
      }
    }
  }

  /**
   * Convenience overload for rendering a plain `numerator / denominator`
   * string pair as a fraction image — the pre-0.3.0 code path, preserved
   * so older call sites keep working unchanged.
   */
  static func fractionImage(
    numerator: String,
    denominator: String,
    font: UIFont,
    color: UIColor,
    barWidth: CGFloat?
  ) -> UIImage {
    fractionImage(
      numeratorRuns: [["type": "text", "text": numerator]],
      denominatorRuns: [["type": "text", "text": denominator]],
      font: font,
      color: color,
      barWidth: barWidth
    )
  }

  /**
   * Core fraction layout. Draws numerator on top, vinculum in the middle,
   * denominator below. The numerator / denominator are themselves rendered
   * by recursively calling {@link image(forRuns:font:color:barWidth:)} so
   * they may contain text, nested fractions, or scripts.
   */
  static func fractionImage(
    numeratorRuns: [Any],
    denominatorRuns: [Any],
    font: UIFont,
    color: UIColor,
    barWidth: CGFloat?
  ) -> UIImage {
    let fracFont = UIFont(
      descriptor: font.fontDescriptor,
      size: font.pointSize * 0.75
    )
    let numImage = image(
      forRuns: numeratorRuns,
      font: fracFont,
      color: color,
      barWidth: barWidth
    )
    let denImage = image(
      forRuns: denominatorRuns,
      font: fracFont,
      color: color,
      barWidth: barWidth
    )

    let gap: CGFloat = 1
    let resolvedBarWidth: CGFloat =
      barWidth.map { max(0.5, $0) } ?? max(1, fracFont.pointSize * 0.06)
    let sidePadding: CGFloat = 3
    let totalWidth = max(numImage.size.width, denImage.size.width) + sidePadding * 2
    let totalHeight =
      numImage.size.height + denImage.size.height + gap * 2 + resolvedBarWidth

    let format = UIGraphicsImageRendererFormat.default()
    format.opaque = false
    let renderer = UIGraphicsImageRenderer(
      size: CGSize(width: totalWidth, height: totalHeight),
      format: format
    )
    return renderer.image { ctx in
      let cg = ctx.cgContext

      numImage.draw(
        in: CGRect(
          x: (totalWidth - numImage.size.width) / 2,
          y: 0,
          width: numImage.size.width,
          height: numImage.size.height
        )
      )

      cg.setStrokeColor(color.cgColor)
      cg.setLineWidth(resolvedBarWidth)
      let barY = numImage.size.height + gap + resolvedBarWidth / 2
      cg.move(to: CGPoint(x: sidePadding, y: barY))
      cg.addLine(to: CGPoint(x: totalWidth - sidePadding, y: barY))
      cg.strokePath()

      denImage.draw(
        in: CGRect(
          x: (totalWidth - denImage.size.width) / 2,
          y: numImage.size.height + gap * 2 + resolvedBarWidth,
          width: denImage.size.width,
          height: denImage.size.height
        )
      )
    }
  }

  // MARK: - Private helpers

  private static func renderText(
    _ text: String,
    font: UIFont,
    color: UIColor
  ) -> (image: UIImage, baseline: CGFloat) {
    let attrs: [NSAttributedString.Key: Any] = [
      .font: font,
      .foregroundColor: color,
    ]
    let attr = NSAttributedString(string: text, attributes: attrs)
    let size = attr.size()
    let format = UIGraphicsImageRendererFormat.default()
    format.opaque = false
    let renderer = UIGraphicsImageRenderer(size: size, format: format)
    let img = renderer.image { _ in
      attr.draw(in: CGRect(origin: .zero, size: size))
    }
    return (img, font.ascender)
  }

  private static func renderFraction(
    _ dict: [String: Any],
    font: UIFont,
    color: UIColor,
    barWidth: CGFloat?
  ) -> (image: UIImage, baseline: CGFloat) {
    let numRuns = dict["numeratorRuns"] as? [Any]
    let denRuns = dict["denominatorRuns"] as? [Any]
    let numString = dict["numerator"] as? String ?? ""
    let denString = dict["denominator"] as? String ?? ""

    let img = fractionImage(
      numeratorRuns: numRuns ?? [["type": "text", "text": numString]],
      denominatorRuns: denRuns ?? [["type": "text", "text": denString]],
      font: font,
      color: color,
      barWidth: barWidth
    )
    // Center the fraction on the host x-height, mirroring
    // `FractionAttachment.attachmentBounds` — the baseline sits
    // `xHeight/2 + height/2` below the top.
    let xHeight = font.xHeight
    let baseline = img.size.height / 2 + xHeight / 2
    return (img, baseline)
  }

  private static func renderScript(
    _ runs: [Any],
    font: UIFont,
    color: UIColor,
    barWidth: CGFloat?,
    raise: Bool
  ) -> (image: UIImage, baseline: CGFloat) {
    let scriptFont = UIFont(
      descriptor: font.fontDescriptor,
      size: font.pointSize * 0.65
    )
    let img = image(
      forRuns: runs,
      font: scriptFont,
      color: color,
      barWidth: barWidth
    )
    // Raised baseline for super, lowered for sub. `capHeight * 0.45`
    // matches typical typographic superscript offset.
    let offset = font.capHeight * 0.45
    let baseline: CGFloat
    if raise {
      baseline = scriptFont.ascender + offset
    } else {
      baseline = scriptFont.ascender - offset
    }
    return (img, baseline)
  }
}

/**
 * `NSTextAttachment` subclass that renders a stacked fraction. Retained as
 * the public surface so existing call sites
 * `FractionAttachment(numerator:denominator:…)` compile unchanged; new
 * call sites can use {@link FractionAttachment.init(numeratorRuns:denominatorRuns:…)}
 * to pass structured numerator / denominator arrays that may themselves
 * contain nested fractions, scripts, or text (library schema 0.3.0+).
 */
final class FractionAttachment: NSTextAttachment {
  private let hostFont: UIFont

  init(
    numerator: String,
    denominator: String,
    font: UIFont,
    color: UIColor,
    overrideBarWidth: CGFloat? = nil
  ) {
    self.hostFont = font
    super.init(data: nil, ofType: nil)
    self.image = FractionRenderer.fractionImage(
      numerator: numerator,
      denominator: denominator,
      font: font,
      color: color,
      barWidth: overrideBarWidth
    )
  }

  init(
    numeratorRuns: [Any],
    denominatorRuns: [Any],
    font: UIFont,
    color: UIColor,
    overrideBarWidth: CGFloat? = nil
  ) {
    self.hostFont = font
    super.init(data: nil, ofType: nil)
    self.image = FractionRenderer.fractionImage(
      numeratorRuns: numeratorRuns,
      denominatorRuns: denominatorRuns,
      font: font,
      color: color,
      barWidth: overrideBarWidth
    )
  }

  required init?(coder: NSCoder) {
    fatalError("FractionAttachment cannot be coder-initialised")
  }

  override func attachmentBounds(
    for textContainer: NSTextContainer?,
    proposedLineFragment lineFrag: CGRect,
    glyphPosition position: CGPoint,
    characterIndex charIndex: Int
  ) -> CGRect {
    guard let img = self.image else { return .zero }
    let size = img.size
    let xHeight = hostFont.xHeight
    let y = xHeight / 2 - size.height / 2
    return CGRect(x: 0, y: y, width: size.width, height: size.height)
  }
}

/**
 * Shared attachment for raised- or lowered-baseline content. Rendered by
 * composing the child runs onto a `UIImage` via {@link FractionRenderer}
 * at 0.65× host font size, then placing the image above (super) or below
 * (sub) the text baseline by `capHeight * 0.45`.
 *
 * Library schema 0.3.0+.
 */
final class ScriptAttachment: NSTextAttachment {
  enum Kind { case superscript, `subscript` }

  private let hostFont: UIFont
  private let kind: Kind
  private let contentHeight: CGFloat

  init(
    runs: [Any],
    font: UIFont,
    color: UIColor,
    barWidth: CGFloat?,
    kind: Kind
  ) {
    self.hostFont = font
    self.kind = kind
    let scriptFont = UIFont(
      descriptor: font.fontDescriptor,
      size: font.pointSize * 0.65
    )
    let img = FractionRenderer.image(
      forRuns: runs,
      font: scriptFont,
      color: color,
      barWidth: barWidth
    )
    self.contentHeight = img.size.height
    super.init(data: nil, ofType: nil)
    self.image = img
  }

  required init?(coder: NSCoder) {
    fatalError("ScriptAttachment cannot be coder-initialised")
  }

  override func attachmentBounds(
    for textContainer: NSTextContainer?,
    proposedLineFragment lineFrag: CGRect,
    glyphPosition position: CGPoint,
    characterIndex charIndex: Int
  ) -> CGRect {
    guard let img = self.image else { return .zero }
    let offset = hostFont.capHeight * 0.45
    let y: CGFloat
    switch kind {
    case .superscript:
      // Raise so the content sits above the x-height line.
      y = hostFont.capHeight - contentHeight + offset
    case .subscript:
      // Lower so the content dips below the baseline.
      y = -offset
    }
    return CGRect(x: 0, y: y, width: img.size.width, height: img.size.height)
  }
}
