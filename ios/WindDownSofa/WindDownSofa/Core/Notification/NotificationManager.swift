import Foundation
import UserNotifications

// MARK: - NotificationManager (PRD §4)

final class NotificationManager: ObservableObject {
    static let shared = NotificationManager()

    @Published var permissionStatus: UNAuthorizationStatus = .notDetermined

    private init() {
        checkPermissionStatus()
    }

    // MARK: - Identifiers

    enum CategoryIdentifier {
        static let bedtimeReminder = "BEDTIME_REMINDER"
        static let bedtimeReminderFinal = "BEDTIME_REMINDER_FINAL"
    }

    enum ActionIdentifier {
        static let sleep = "SLEEP_ACTION"
        static let snooze = "SNOOZE_ACTION"
    }

    // MARK: - Setup

    func setupNotificationCategories() {
        let sleepAction = UNNotificationAction(
            identifier: ActionIdentifier.sleep,
            title: "我準備睡了 😴",
            options: [.foreground]
        )
        let snoozeAction = UNNotificationAction(
            identifier: ActionIdentifier.snooze,
            title: "再給我 15 分 🛋️",
            options: []
        )

        let standardCategory = UNNotificationCategory(
            identifier: CategoryIdentifier.bedtimeReminder,
            actions: [sleepAction, snoozeAction],
            intentIdentifiers: [],
            options: []
        )

        let finalCategory = UNNotificationCategory(
            identifier: CategoryIdentifier.bedtimeReminderFinal,
            actions: [sleepAction],
            intentIdentifiers: [],
            options: []
        )

        UNUserNotificationCenter.current().setNotificationCategories([standardCategory, finalCategory])
    }

    // MARK: - Permission

    func requestPermission(completion: @escaping (Bool) -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            DispatchQueue.main.async {
                AppSettings.shared.notificationPermissionRequested = true
                self.checkPermissionStatus()
                completion(granted)
            }
        }
    }

    func checkPermissionStatus() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                self.permissionStatus = settings.authorizationStatus
            }
        }
    }

    // MARK: - Schedule Bedtime Reminder (PRD §4.1)

    func scheduleBedtimeReminder(targetTimeString: String, leadMinutes: Int = 30) {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()

        guard let reminderDate = computeReminderDate(targetTimeString: targetTimeString, leadMinutes: leadMinutes) else {
            return
        }

        let content = makeBedtimeContent(targetTimeString: targetTimeString, snoozeCount: 0)
        let trigger = UNCalendarNotificationTrigger(
            dateMatching: Calendar.current.dateComponents([.hour, .minute], from: reminderDate),
            repeats: true
        )

        let request = UNNotificationRequest(
            identifier: "bedtime_reminder",
            content: content,
            trigger: trigger
        )

        UNUserNotificationCenter.current().add(request)
    }

    // MARK: - Snooze Reminder (PRD §4.3)

    func scheduleSnoozeReminder(snoozeCount: Int) {
        let isFinalSnooze = snoozeCount >= 2

        let content = makeSnoozeContent(snoozeCount: snoozeCount, isFinal: isFinalSnooze)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 15 * 60, repeats: false)

        let request = UNNotificationRequest(
            identifier: "snooze_reminder_\(snoozeCount)",
            content: content,
            trigger: trigger
        )

        UNUserNotificationCenter.current().add(request)
    }

    // MARK: - Cancel

    func cancelAllNotifications() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }

    // MARK: - Private Helpers

    private func computeReminderDate(targetTimeString: String, leadMinutes: Int) -> Date? {
        let components = targetTimeString.split(separator: ":").compactMap { Int($0) }
        guard components.count == 2 else { return nil }

        var cal = Calendar.current
        cal.timeZone = TimeZone.current

        var dc = cal.dateComponents([.year, .month, .day], from: Date())
        dc.hour = components[0]
        dc.minute = components[1]
        dc.second = 0

        guard var targetDate = cal.date(from: dc) else { return nil }

        let reminderDate = targetDate.addingTimeInterval(TimeInterval(-leadMinutes * 60))

        if reminderDate <= Date() {
            targetDate = cal.date(byAdding: .day, value: 1, to: targetDate) ?? targetDate
            return targetDate.addingTimeInterval(TimeInterval(-leadMinutes * 60))
        }

        return reminderDate
    }

    private func makeBedtimeContent(targetTimeString: String, snoozeCount: Int) -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        content.title = "嘿，還在滑嗎？🌙"
        content.body = "你設定今晚 \(targetTimeString) 睡，現在是時候準備了"
        content.sound = .default
        content.categoryIdentifier = CategoryIdentifier.bedtimeReminder
        content.userInfo = ["snoozeCount": snoozeCount]
        return content
    }

    private func makeSnoozeContent(snoozeCount: Int, isFinal: Bool) -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        content.title = "嘿，還在滑嗎？🌙"
        content.body = isFinal ? "時間到了，真的該睡了 😴" : "15 分鐘過了，準備好了嗎？"
        content.sound = .default
        content.categoryIdentifier = isFinal
            ? CategoryIdentifier.bedtimeReminderFinal
            : CategoryIdentifier.bedtimeReminder
        content.userInfo = ["snoozeCount": snoozeCount]
        return content
    }
}
