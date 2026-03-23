import Foundation
import Combine

// MARK: - HomeViewModel

final class HomeViewModel: ObservableObject {
    @Published var targetTimeString: String
    @Published var selectedMethodId: String
    @Published var streak: Int
    @Published var leadMinutes: Int

    private let settings = AppSettings.shared
    private var cancellables = Set<AnyCancellable>()

    init() {
        self.targetTimeString = AppSettings.shared.defaultSleepTime
        self.selectedMethodId = AppSettings.shared.selectedMethodId
        self.streak = AppSettings.shared.streakCount
        self.leadMinutes = AppSettings.shared.reminderLeadMinutes

        bindSettings()
    }

    // MARK: - Actions

    func updateTargetTime(_ timeString: String) {
        settings.defaultSleepTime = timeString
        targetTimeString = timeString
        rescheduleNotification()
    }

    func updateSelectedMethod(_ methodId: String) {
        settings.selectedMethodId = methodId
        selectedMethodId = methodId
    }

    // MARK: - Private

    private func rescheduleNotification() {
        NotificationManager.shared.scheduleBedtimeReminder(
            targetTimeString: targetTimeString,
            leadMinutes: leadMinutes
        )
    }

    private func bindSettings() {
        settings.$streakCount
            .receive(on: RunLoop.main)
            .assign(to: \.streak, on: self)
            .store(in: &cancellables)

        settings.$reminderLeadMinutes
            .receive(on: RunLoop.main)
            .assign(to: \.leadMinutes, on: self)
            .store(in: &cancellables)
    }
}
