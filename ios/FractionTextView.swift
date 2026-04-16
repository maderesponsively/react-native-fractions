import UIKit
import React

@objc(FractionTextView)
public class FractionTextView: UIView {

  private let label = UILabel()
  private var dirty = true
  private var lastReportedWidth: CGFloat = -1
  private var lastReportedHeight: CGFloat = -1

  @objc public var runs: NSArray = [] {
    didSet { setNeedsRebuild() }
  }
  @objc public var fontSize: NSNumber = NSNumber(value: 14) {
    didSet { setNeedsRebuild() }
  }
  @objc public var lineHeight: NSNumber = NSNumber(value: 20) {
    didSet { setNeedsRebuild() }
  }
  @objc public var color: UIColor? {
    didSet { setNeedsRebuild() }
  }
  @objc public var fontFamily: NSString? {
    didSet { setNeedsRebuild() }
  }
  @objc public var fontWeight: NSString? {
    didSet { setNeedsRebuild() }
  }
  @objc public var textAlign: NSString? {
    didSet { setNeedsRebuild() }
  }
  @objc public var onContentSizeChange: RCTDirectEventBlock?

  public override init(frame: CGRect) {
    super.init(frame: frame)
    label.numberOfLines = 0
    label.lineBreakMode = .byWordWrapping
    label.adjustsFontForContentSizeCategory = false
    label.isAccessibilityElement = true
    isUserInteractionEnabled = false
    clipsToBounds = false
    addSubview(label)
  }

  required init?(coder: NSCoder) {
    fatalError("FractionTextView cannot be coder-initialised")
  }

  public override func layoutSubviews() {
    super.layoutSubviews()
    if dirty {
      rebuildAttributedText()
      dirty = false
    }
    label.frame = bounds
    notifyContentSizeIfNeeded()
  }

  private func setNeedsRebuild() {
    dirty = true
    setNeedsLayout()
  }

  private func resolveFont() -> UIFont {
    let size = CGFloat(truncating: fontSize)
    let family = (fontFamily as String?).flatMap { $0.isEmpty ? nil : $0 }
    let weight = (fontWeight as String?).flatMap { $0.isEmpty ? nil : $0 }
    let base = UIFont.systemFont(ofSize: size)
    if let updated = RCTFont.update(
      base,
      withFamily: family,
      size: NSNumber(value: Double(size)),
      weight: weight,
      style: nil,
      variant: nil,
      scaleMultiplier: 1.0
    ) {
      return updated
    }
    return base
  }

  private func resolveColor() -> UIColor {
    if let c = color { return c }
    return label.textColor ?? .label
  }

  private func resolveAlignment() -> NSTextAlignment {
    switch (textAlign as String?) ?? "" {
    case "center": return .center
    case "right": return .right
    case "left": return .left
    default: return .natural
    }
  }

  private func rebuildAttributedText() {
    let font = resolveFont()
    let textColor = resolveColor()
    let lineHeightValue = CGFloat(truncating: lineHeight)

    let paragraph = NSMutableParagraphStyle()
    paragraph.alignment = resolveAlignment()
    paragraph.minimumLineHeight = lineHeightValue
    paragraph.lineBreakMode = .byWordWrapping

    let baseAttributes: [NSAttributedString.Key: Any] = [
      .font: font,
      .foregroundColor: textColor,
      .paragraphStyle: paragraph,
    ]

    let result = NSMutableAttributedString()
    for raw in runs {
      guard let dict = raw as? [String: Any],
            let type = dict["type"] as? String else { continue }
      if type == "text" {
        let text = dict["text"] as? String ?? ""
        if text.isEmpty { continue }
        result.append(NSAttributedString(string: text, attributes: baseAttributes))
      } else if type == "fraction" {
        let numerator = dict["numerator"] as? String ?? ""
        let denominator = dict["denominator"] as? String ?? ""
        let attachment = FractionAttachment(
          numerator: numerator,
          denominator: denominator,
          font: font,
          color: textColor
        )
        let attr = NSMutableAttributedString(attachment: attachment)
        attr.addAttributes(
          baseAttributes,
          range: NSRange(location: 0, length: attr.length)
        )
        result.append(attr)
      }
    }

    label.attributedText = result
    label.textAlignment = resolveAlignment()
    lastReportedWidth = -1
    lastReportedHeight = -1
  }

  private func notifyContentSizeIfNeeded() {
    guard bounds.width > 0 else { return }
    let fit = label.sizeThatFits(
      CGSize(width: bounds.width, height: .greatestFiniteMagnitude)
    )
    if abs(fit.width - lastReportedWidth) > 0.5
      || abs(fit.height - lastReportedHeight) > 0.5
    {
      lastReportedWidth = fit.width
      lastReportedHeight = fit.height
      onContentSizeChange?([
        "width": fit.width,
        "height": fit.height,
      ])
    }
  }
}
