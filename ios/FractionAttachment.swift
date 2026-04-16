import UIKit

final class FractionAttachment: NSTextAttachment {
  private let numerator: String
  private let denominator: String
  private let hostFont: UIFont
  private let textColor: UIColor

  init(numerator: String, denominator: String, font: UIFont, color: UIColor) {
    self.numerator = numerator
    self.denominator = denominator
    self.hostFont = font
    self.textColor = color
    super.init(data: nil, ofType: nil)
    self.image = renderImage()
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

  private func renderImage() -> UIImage {
    let fracFontSize = hostFont.pointSize * 0.75
    let fracFont: UIFont = UIFont(
      descriptor: hostFont.fontDescriptor,
      size: fracFontSize
    )

    let attrs: [NSAttributedString.Key: Any] = [
      .font: fracFont,
      .foregroundColor: textColor,
    ]
    let numAttr = NSAttributedString(string: numerator, attributes: attrs)
    let denAttr = NSAttributedString(string: denominator, attributes: attrs)

    let numSize = numAttr.size()
    let denSize = denAttr.size()
    let gap: CGFloat = 1
    let barWidth: CGFloat = max(1, fracFontSize * 0.06)
    let sidePadding: CGFloat = 3

    let totalWidth = max(numSize.width, denSize.width) + sidePadding * 2
    let totalHeight = numSize.height + denSize.height + gap * 2 + barWidth

    let rendererFormat = UIGraphicsImageRendererFormat.default()
    rendererFormat.opaque = false
    let renderer = UIGraphicsImageRenderer(
      size: CGSize(width: totalWidth, height: totalHeight),
      format: rendererFormat
    )
    return renderer.image { ctx in
      let cg = ctx.cgContext

      let numRect = CGRect(
        x: (totalWidth - numSize.width) / 2,
        y: 0,
        width: numSize.width,
        height: numSize.height
      )
      numAttr.draw(in: numRect)

      cg.setStrokeColor(textColor.cgColor)
      cg.setLineWidth(barWidth)
      let barY = numSize.height + gap + barWidth / 2
      cg.move(to: CGPoint(x: sidePadding, y: barY))
      cg.addLine(to: CGPoint(x: totalWidth - sidePadding, y: barY))
      cg.strokePath()

      let denRect = CGRect(
        x: (totalWidth - denSize.width) / 2,
        y: numSize.height + gap * 2 + barWidth,
        width: denSize.width,
        height: denSize.height
      )
      denAttr.draw(in: denRect)
    }
  }
}
