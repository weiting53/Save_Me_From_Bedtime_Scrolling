import Foundation
import SwiftUI

// MARK: - WindDownViewModel

final class WindDownViewModel: ObservableObject {
    @Published var isEntranceComplete = false
    @Published var showShortcutsSetup = false
    @Published var moonScale: CGFloat = 0.3
    @Published var moonOpacity: Double = 0
    @Published var contentOpacity: Double = 0

    let selectedMethod: AnyWindDownMethod
    private let isFirstWindDown: Bool

    init() {
        let methodId = AppSettings.shared.selectedMethodId
        self.selectedMethod = MethodRegistry.shared.method(for: methodId)
            ?? MethodRegistry.shared.allMethods[0]
        self.isFirstWindDown = !UserDefaults.standard.bool(forKey: "hasSeenWindDownShortcuts")
    }

    func startEntrance() {
        StorageManager.shared.recordWindDownActivated()

        // Moon rises (1.5s, not skippable per PRD §3.2)
        withAnimation(.easeOut(duration: 0.8)) {
            moonScale = 1.0
            moonOpacity = 1.0
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            withAnimation(AppAnimation.screenTransition) {
                self.isEntranceComplete = true
            }
            withAnimation(.easeOut(duration: 0.5).delay(0.2)) {
                self.contentOpacity = 1.0
            }
            if self.isFirstWindDown {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                    self.showShortcutsSetup = true
                    UserDefaults.standard.set(true, forKey: "hasSeenWindDownShortcuts")
                }
            }
        }
    }

    func launchShortcuts() {
        let urlString = "shortcuts://run-shortcut?name=WindDown"
        guard let url = URL(string: urlString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? urlString) else { return }
        UIApplication.shared.open(url)
    }
}
