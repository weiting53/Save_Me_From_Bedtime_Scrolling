import SwiftUI

// MARK: - ReminderTimeCardView (PRD §3.1)

struct ReminderTimeCardView: View {
    let targetTimeString: String
    let leadMinutes: Int

    var reminderTimeString: String {
        let components = targetTimeString.split(separator: ":").compactMap { Int($0) }
        guard components.count == 2 else { return "--:--" }

        var dc = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        dc.hour = components[0]
        dc.minute = components[1]
        guard let targetDate = Calendar.current.date(from: dc) else { return "--:--" }

        let reminderDate = targetDate.addingTimeInterval(TimeInterval(-leadMinutes * 60))
        return DateFormatter.timeOnly.string(from: reminderDate)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("今晚提醒時間")
                .cardLabel()

            HStack(alignment: .firstTextBaseline) {
                Text(reminderTimeString)
                    .font(AppFont.timeDisplay())
                    .foregroundColor(Color.textPrimary)

                Spacer()

                Image(systemName: "bell.fill")
                    .foregroundColor(Color.textSecondary)
                    .font(.system(size: 16))
            }

            Text("提前 \(leadMinutes) 分鐘通知你")
                .font(AppFont.bodyText())
                .foregroundColor(Color.textSecondary)
        }
        .cardStyle()
    }
}
