import Foundation
import UserNotifications
import Combine

// MARK: - SettingsViewModel

final class SettingsViewModel: ObservableObject {
    @Published var defaultSleepTime: String
    @Published var reminderLeadMinutes: Int
    @Published var notificationStatus: UNAuthorizationStatus = .notDetermined
    @Published var showResetConfirm = false

    let leadMinuteOptions = [15, 30, 45, 60]

    private let settings = AppSettings.shared
    private var cancellables = Set<AnyCancellable>()

    init() {
        self.defaultSleepTime = AppSettings.shared.defaultSleepTime
        self.reminderLeadMinutes = AppSettings.shared.reminderLeadMinutes
        bindNotificationStatus()
    }

    func updateDefaultSleepTime(_ time: String) {
        settings.defaultSleepTime = time
        defaultSleepTime = time
        rescheduleNotification()
    }

    func updateLeadMinutes(_ minutes: Int) {
        settings.reminderLeadMinutes = minutes
        reminderLeadMinutes = minutes
        rescheduleNotification()
    }

    func openSystemSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }

    func resetAllData() {
        StorageManager.shared.resetAllData()
        defaultSleepTime = AppSettings.shared.defaultSleepTime
        reminderLeadMinutes = AppSettings.shared.reminderLeadMinutes
        NotificationManager.shared.cancelAllNotifications()
    }

    func refreshNotificationStatus() {
        NotificationManager.shared.checkPermissionStatus()
    }

    private func rescheduleNotification() {
        NotificationManager.shared.scheduleBedtimeReminder(
            targetTimeString: defaultSleepTime,
            leadMinutes: reminderLeadMinutes
        )
    }

    private func bindNotificationStatus() {
        NotificationManager.shared.$permissionStatus
            .receive(on: RunLoop.main)
            .assign(to: \.notificationStatus, on: self)
            .store(in: &cancellables)
    }
}
