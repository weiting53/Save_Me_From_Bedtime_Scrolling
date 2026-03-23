import UIKit
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        NotificationManager.shared.setupNotificationCategories()
        StreakManager.shared.evaluateStreakOnLaunch()
        return true
    }

    // MARK: - UNUserNotificationCenterDelegate

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let actionIdentifier = response.actionIdentifier

        switch actionIdentifier {
        case NotificationManager.ActionIdentifier.sleep:
            handleSleepAction()
        case NotificationManager.ActionIdentifier.snooze:
            handleSnoozeAction(response: response)
        default:
            break
        }
        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    // MARK: - Private

    private func handleSleepAction() {
        StorageManager.shared.recordUserChoice(.sleep)
        NotificationCenter.default.post(name: .openWindDown, object: nil)
    }

    private func handleSnoozeAction(response: UNNotificationResponse) {
        let userInfo = response.notification.request.content.userInfo
        let currentSnoozeCount = userInfo["snoozeCount"] as? Int ?? 0
        StorageManager.shared.recordSnooze(snoozeCount: currentSnoozeCount + 1)
        NotificationManager.shared.scheduleSnoozeReminder(snoozeCount: currentSnoozeCount + 1)
    }
}

extension Notification.Name {
    static let openWindDown = Notification.Name("openWindDown")
}
