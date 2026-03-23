import SwiftUI

// MARK: - MethodRegistry (PRD §3.5)

final class MethodRegistry: ObservableObject {
    static let shared = MethodRegistry()

    let allMethods: [AnyWindDownMethod]

    private init() {
        self.allMethods = [
            AnyWindDownMethod(Breathing478Method()),
            AnyWindDownMethod(DailyReflectionMethod()),
        ]
    }

    func method(for id: String) -> AnyWindDownMethod? {
        allMethods.first { $0.id == id }
    }

    func randomMethod(excluding currentId: String? = nil) -> AnyWindDownMethod {
        let candidates = allMethods.filter { $0.id != currentId }
        return candidates.randomElement() ?? allMethods[0]
    }
}

// MARK: - Type-erased wrapper

struct AnyWindDownMethod: WindDownMethod {
    let id: String
    let name: String
    let icon: String
    let description: String
    private let _view: () -> AnyView

    init<M: WindDownMethod>(_ method: M) {
        self.id = method.id
        self.name = method.name
        self.icon = method.icon
        self.description = method.description
        self._view = { method.view() }
    }

    func view() -> AnyView {
        _view()
    }
}

// MARK: - Method Definitions

struct Breathing478Method: WindDownMethod {
    let id = "breathing_478"
    let name = "4-7-8 呼吸法"
    let icon = "wind"
    let description = "吸氣 4 秒、屏息 7 秒、吐氣 8 秒，讓神經系統放鬆"

    func view() -> AnyView {
        AnyView(Breathing478View())
    }
}

struct DailyReflectionMethod: WindDownMethod {
    let id = "daily_reflection"
    let name = "今日反思"
    let icon = "pencil.and.list.clipboard"
    let description = "用三個簡單問題，為今天畫下句點"

    func view() -> AnyView {
        AnyView(DailyReflectionView())
    }
}
