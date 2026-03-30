import Foundation
import UserNotifications

struct NotificationManager {

    static let morningNotifID = "scrollingdead.morning"

    // MARK: - Permission

    static func requestPermission() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound])
        } catch {
            return false
        }
    }

    // MARK: - Schedule morning insight

    /// Call this each night after the user's session ends (or on app launch).
    /// - Parameters:
    ///   - bedtime: The user's target bedtime (hour + minute, 24h)
    ///   - actualEndMinutes: How many minutes past bedtime the user actually used the phone.
    ///                       Pass 0 if unknown or first run.
    static func scheduleMorningInsight(bedtimeHour: Int, bedtimeMinute: Int, overMinutes: Int = 0) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [morningNotifID])

        let content        = UNMutableNotificationContent()
        content.title      = L.morningTitle
        content.body       = L.morningBody(overMinutes: overMinutes)
        content.sound      = .default

        // Fire next morning at 8:00 AM
        var comps          = DateComponents()
        comps.hour         = 8
        comps.minute       = 0
        let trigger        = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)

        let request = UNNotificationRequest(
            identifier: morningNotifID,
            content:    content,
            trigger:    trigger
        )
        center.add(request)
    }

    // MARK: - Cancel all

    static func cancelAll() {
        UNUserNotificationCenter.current()
            .removeAllPendingNotificationRequests()
    }
}
