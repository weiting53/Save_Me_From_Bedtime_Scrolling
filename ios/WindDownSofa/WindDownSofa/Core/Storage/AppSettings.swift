import Foundation
import Combine

// MARK: - AppSettings (PRD §5.2)

final class AppSettings: ObservableObject {
    static let shared = AppSettings()

    @Published var defaultSleepTime: String {
        didSet { UserDefaults.standard.set(defaultSleepTime, forKey: Keys.defaultSleepTime) }
    }

    @Published var reminderLeadMinutes: Int {
        didSet { UserDefaults.standard.set(reminderLeadMinutes, forKey: Keys.reminderLeadMinutes) }
    }

    @Published var selectedMethodId: String {
        didSet { UserDefaults.standard.set(selectedMethodId, forKey: Keys.selectedMethodId) }
    }

    @Published var streakCount: Int {
        didSet { UserDefaults.standard.set(streakCount, forKey: Keys.streakCount) }
    }

    @Published var lastStreakDate: String {
        didSet { UserDefaults.standard.set(lastStreakDate, forKey: Keys.lastStreakDate) }
    }

    @Published var hasCompletedOnboarding: Bool {
        didSet { UserDefaults.standard.set(hasCompletedOnboarding, forKey: Keys.hasCompletedOnboarding) }
    }

    @Published var notificationPermissionRequested: Bool {
        didSet { UserDefaults.standard.set(notificationPermissionRequested, forKey: Keys.notificationPermissionRequested) }
    }

    private init() {
        let ud = UserDefaults.standard
        self.defaultSleepTime = ud.string(forKey: Keys.defaultSleepTime) ?? "00:00"
        self.reminderLeadMinutes = ud.object(forKey: Keys.reminderLeadMinutes) as? Int ?? 30
        self.selectedMethodId = ud.string(forKey: Keys.selectedMethodId) ?? "breathing_478"
        self.streakCount = ud.integer(forKey: Keys.streakCount)
        self.lastStreakDate = ud.string(forKey: Keys.lastStreakDate) ?? ""
        self.hasCompletedOnboarding = ud.bool(forKey: Keys.hasCompletedOnboarding)
        self.notificationPermissionRequested = ud.bool(forKey: Keys.notificationPermissionRequested)
    }

    func resetAll() {
        defaultSleepTime = "00:00"
        reminderLeadMinutes = 30
        selectedMethodId = "breathing_478"
        streakCount = 0
        lastStreakDate = ""
        notificationPermissionRequested = false
    }

    enum Keys {
        static let defaultSleepTime = "defaultSleepTime"
        static let reminderLeadMinutes = "reminderLeadMinutes"
        static let selectedMethodId = "selectedMethodId"
        static let streakCount = "streakCount"
        static let lastStreakDate = "lastStreakDate"
        static let hasCompletedOnboarding = "hasCompletedOnboarding"
        static let notificationPermissionRequested = "notificationPermissionRequested"
    }
}
