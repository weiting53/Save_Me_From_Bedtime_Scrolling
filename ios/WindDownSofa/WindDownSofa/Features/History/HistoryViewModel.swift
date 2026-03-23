import Foundation
import Combine

// MARK: - HistoryViewModel

final class HistoryViewModel: ObservableObject {
    @Published var last7Days: [DayRecord] = []
    @Published var weekSummary: WeekSummary? = nil

    struct DayRecord: Identifiable {
        let id: String
        let date: String
        let displayDate: String
        let shortDay: String
        let choice: SleepLog.UserChoice
        let targetTime: String
        let windDownActivated: Bool
        let sleepValue: Double
    }

    struct WeekSummary {
        let sleepDays: Int
        let previousWeekSleepDays: Int
        var text: String {
            let improvement = sleepDays - previousWeekSleepDays
            let improvementText = improvement > 0
                ? "，比上週進步了 \(improvement) 天 👍"
                : improvement == 0 ? "，和上週一樣 💪" : ""
            return "本週你有 \(sleepDays) 天提早入睡\(improvementText)"
        }
    }

    func refresh() {
        let logs = StorageManager.shared.logs(forLast: 7)
        let calendar = Calendar.current

        last7Days = (0..<7).map { daysAgo -> DayRecord in
            let date = calendar.date(byAdding: .day, value: -daysAgo, to: Date()) ?? Date()
            let dateString = DateFormatter.dateOnly.string(from: date)

            let log = logs.first { $0.date == dateString }
            let choice = log?.userChoice ?? .pending
            let sleepValue: Double = choice == .sleep ? 1.0 : 0.0

            let dayFormatter = DateFormatter()
            dayFormatter.dateFormat = "EEE"
            dayFormatter.locale = Locale(identifier: "zh_TW")

            return DayRecord(
                id: dateString,
                date: dateString,
                displayDate: dateString,
                shortDay: dayFormatter.string(from: date),
                choice: choice,
                targetTime: log?.targetSleepTime ?? "--:--",
                windDownActivated: log?.windDownActivated ?? false,
                sleepValue: sleepValue
            )
        }.reversed()

        computeWeekSummary(logs: logs)
    }

    private func computeWeekSummary(logs: [SleepLog]) {
        let calendar = Calendar.current
        let sleepDays = logs.filter { $0.userChoice == .sleep }.count

        let previousLogs = StorageManager.shared.logs(forLast: 14).filter { log in
            guard let date = DateFormatter.dateOnly.date(from: log.date) else { return false }
            let daysAgo = calendar.dateComponents([.day], from: date, to: Date()).day ?? 0
            return daysAgo >= 7 && daysAgo < 14
        }
        let previousSleepDays = previousLogs.filter { $0.userChoice == .sleep }.count

        if sleepDays > 0 || previousSleepDays > 0 {
            weekSummary = WeekSummary(
                sleepDays: sleepDays,
                previousWeekSleepDays: previousSleepDays
            )
        } else {
            weekSummary = nil
        }
    }
}
