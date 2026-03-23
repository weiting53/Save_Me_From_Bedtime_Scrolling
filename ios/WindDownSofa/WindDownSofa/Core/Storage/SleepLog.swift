import Foundation

// MARK: - SleepLog (PRD §5.1)

struct SleepLog: Codable, Identifiable {
    var id: String { date }

    let date: String
    var targetSleepTime: String
    var reminderFiredAt: Date?
    var userChoice: UserChoice
    var snoozeCount: Int
    var windDownActivated: Bool
    var selectedMethod: String

    enum UserChoice: String, Codable {
        case sleep
        case snooze
        case ignored
        case pending
    }

    init(
        date: String = DateFormatter.dateOnly.string(from: Date()),
        targetSleepTime: String = "00:00",
        reminderFiredAt: Date? = nil,
        userChoice: UserChoice = .pending,
        snoozeCount: Int = 0,
        windDownActivated: Bool = false,
        selectedMethod: String = "breathing_478"
    ) {
        self.date = date
        self.targetSleepTime = targetSleepTime
        self.reminderFiredAt = reminderFiredAt
        self.userChoice = userChoice
        self.snoozeCount = snoozeCount
        self.windDownActivated = windDownActivated
        self.selectedMethod = selectedMethod
    }
}

// MARK: - DateFormatter Helpers

extension DateFormatter {
    static let dateOnly: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    static let timeOnly: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f
    }()
}
