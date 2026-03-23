import Foundation

// MARK: - StorageManager

final class StorageManager {
    static let shared = StorageManager()
    private let logsKey = "sleep_logs"
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    private init() {
        encoder.dateEncodingStrategy = .iso8601
        decoder.dateDecodingStrategy = .iso8601
    }

    // MARK: - Read

    func allLogs() -> [SleepLog] {
        guard let data = UserDefaults.standard.data(forKey: logsKey),
              let logs = try? decoder.decode([SleepLog].self, from: data)
        else { return [] }
        return logs.sorted { $0.date > $1.date }
    }

    func todayLog() -> SleepLog? {
        let today = DateFormatter.dateOnly.string(from: Date())
        return allLogs().first { $0.date == today }
    }

    func logs(forLast days: Int) -> [SleepLog] {
        let calendar = Calendar.current
        let cutoff = calendar.date(byAdding: .day, value: -(days - 1), to: Date()) ?? Date()
        let cutoffString = DateFormatter.dateOnly.string(from: cutoff)
        return allLogs().filter { $0.date >= cutoffString }
    }

    // MARK: - Write

    func upsertLog(_ log: SleepLog) {
        var logs = allLogs()
        if let idx = logs.firstIndex(where: { $0.date == log.date }) {
            logs[idx] = log
        } else {
            logs.append(log)
        }
        save(logs)
    }

    func recordUserChoice(_ choice: SleepLog.UserChoice) {
        var log = todayLog() ?? makeDefaultTodayLog()
        log.userChoice = choice
        log.reminderFiredAt = Date()
        upsertLog(log)
    }

    func recordSnooze(snoozeCount: Int) {
        var log = todayLog() ?? makeDefaultTodayLog()
        log.userChoice = .snooze
        log.snoozeCount = snoozeCount
        if log.reminderFiredAt == nil { log.reminderFiredAt = Date() }
        upsertLog(log)
    }

    func recordWindDownActivated() {
        var log = todayLog() ?? makeDefaultTodayLog()
        log.windDownActivated = true
        upsertLog(log)
    }

    func ensureTodayLog() {
        if todayLog() == nil {
            upsertLog(makeDefaultTodayLog())
        }
    }

    // MARK: - Reset

    func resetAllData() {
        UserDefaults.standard.removeObject(forKey: logsKey)
        AppSettings.shared.resetAll()
    }

    // MARK: - Private

    private func save(_ logs: [SleepLog]) {
        guard let data = try? encoder.encode(logs) else { return }
        UserDefaults.standard.set(data, forKey: logsKey)
    }

    private func makeDefaultTodayLog() -> SleepLog {
        SleepLog(
            date: DateFormatter.dateOnly.string(from: Date()),
            targetSleepTime: AppSettings.shared.defaultSleepTime,
            selectedMethod: AppSettings.shared.selectedMethodId
        )
    }
}

// MARK: - StreakManager

final class StreakManager {
    static let shared = StreakManager()
    private init() {}

    func evaluateStreakOnLaunch() {
        let settings = AppSettings.shared
        let calendar = Calendar.current
        let today = DateFormatter.dateOnly.string(from: Date())
        let yesterday = DateFormatter.dateOnly.string(
            from: calendar.date(byAdding: .day, value: -1, to: Date()) ?? Date()
        )

        if settings.lastStreakDate == today { return }

        let logs = StorageManager.shared.allLogs()
        let yesterdayLog = logs.first { $0.date == yesterday }

        if let log = yesterdayLog, log.userChoice == .sleep {
            settings.streakCount += 1
        } else if settings.lastStreakDate != yesterday {
            settings.streakCount = 0
        }

        settings.lastStreakDate = today
        StorageManager.shared.ensureTodayLog()
    }
}
