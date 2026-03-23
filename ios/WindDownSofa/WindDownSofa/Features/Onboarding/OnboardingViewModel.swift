import Foundation
import Combine
import SwiftUI

// MARK: - OnboardingViewModel

final class OnboardingViewModel: ObservableObject {
    @Published var currentPage = 0
    @Published var targetSleepTime: Date = OnboardingViewModel.defaultMidnight()
    @Published var selectedMethodId = "breathing_478"
    @Published var notificationGranted = false
    @Published var isFinishing = false

    let totalPages = 5

    var canProceed: Bool {
        currentPage < totalPages - 1
    }

    func nextPage() {
        guard canProceed else { return }
        withAnimation(AppAnimation.screenTransition) {
            currentPage += 1
        }
    }

    func requestNotifications() {
        NotificationManager.shared.requestPermission { granted in
            self.notificationGranted = granted
            self.nextPage()
        }
    }

    func finish() {
        let timeString = DateFormatter.timeOnly.string(from: targetSleepTime)
        AppSettings.shared.defaultSleepTime = timeString
        AppSettings.shared.selectedMethodId = selectedMethodId

        let leadMinutes = AppSettings.shared.reminderLeadMinutes
        NotificationManager.shared.scheduleBedtimeReminder(
            targetTimeString: timeString,
            leadMinutes: leadMinutes
        )

        StorageManager.shared.ensureTodayLog()
        AppSettings.shared.hasCompletedOnboarding = true
    }

    private static func defaultMidnight() -> Date {
        var components = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        components.hour = 0
        components.minute = 0
        return Calendar.current.date(from: components) ?? Date()
    }
}
