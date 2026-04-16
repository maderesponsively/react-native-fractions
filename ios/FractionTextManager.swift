import Foundation
import React

@objc(FractionTextManager)
class FractionTextManager: RCTViewManager {

  override class func requiresMainQueueSetup() -> Bool {
    return true
  }

  override func view() -> UIView! {
    return FractionTextView()
  }
}
