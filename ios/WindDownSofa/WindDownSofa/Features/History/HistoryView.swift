import SwiftUI
import Charts

// MARK: - HistoryView (PRD §3.3)

struct HistoryView: View {
    @StateObject private var vm = HistoryViewModel()

    var body: some View {
        ZStack {
            Color.backgroundPrimary.ignoresSafeArea()

            if vm.last7Days.isEmpty {
                emptyStateView
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        // Header
                        Text("睡眠記錄")
                            .font(AppFont.appName())
                            .foregroundColor(Color.textPrimary)
                            .padding(.horizontal, AppLayout.cardPaddingH)
                            .padding(.top, 60)

                        // Week summary banner
                        if let summary = vm.weekSummary {
                            Text(summary.text)
                                .font(AppFont.bodyText())
                                .foregroundColor(Color.textSecondary)
                                .padding(.horizontal, AppLayout.cardPaddingH + 4)
                                .lineSpacing(4)
                        }

                        // Bar chart
                        sleepBarChart
                            .padding(.horizontal, 16)

                        // Daily log list
                        VStack(spacing: 10) {
                            ForEach(vm.last7Days.reversed()) { record in
                                DailyLogRow(record: record)
                                    .padding(.horizontal, 16)
                            }
                        }
                        .padding(.bottom, 32)
                    }
                }
            }
        }
        .onAppear { vm.refresh() }
    }

    // MARK: - Chart

    private var sleepBarChart: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("本週睡眠達成")
                .cardLabel()
                .padding(.leading, 4)

            Chart(vm.last7Days) { record in
                BarMark(
                    x: .value("星期", record.shortDay),
                    y: .value("達成", record.sleepValue)
                )
                .foregroundStyle(
                    record.choice == .sleep
                        ? Color.accentStar.gradient
                        : Color.backgroundTertiary.gradient
                )
                .cornerRadius(6)
            }
            .chartYScale(domain: 0...1)
            .chartYAxis(.hidden)
            .chartXAxis {
                AxisMarks(values: .automatic) { _ in
                    AxisValueLabel()
                        .foregroundStyle(Color.textSecondary)
                }
            }
            .frame(height: 120)
            .padding(AppLayout.cardPaddingH)
            .background(Color.backgroundSecondary)
            .cornerRadius(AppLayout.cardCornerRadius)
            .overlay(
                RoundedRectangle(cornerRadius: AppLayout.cardCornerRadius)
                    .stroke(Color.cardBorder, lineWidth: AppLayout.cardBorderWidth)
            )
        }
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: 20) {
            FoxMascotView(state: .curious)
                .frame(height: 160)

            Text("還沒有任何記錄")
                .font(AppFont.appName())
                .foregroundColor(Color.textPrimary)

            Text("設定今晚的目標睡眠時間，\n明天就能看到你的第一筆記錄 🌙")
                .font(AppFont.bodyText())
                .foregroundColor(Color.textSecondary)
                .multilineTextAlignment(.center)
                .lineSpacing(6)
        }
        .padding(32)
    }
}

// MARK: - Daily Log Row

private struct DailyLogRow: View {
    let record: HistoryViewModel.DayRecord

    var body: some View {
        HStack(spacing: 14) {
            // Date indicator
            VStack(spacing: 2) {
                Text(record.shortDay)
                    .font(AppFont.cardLabel())
                    .cardLabel()
                Text(shortDate)
                    .font(AppFont.bodyText())
                    .foregroundColor(Color.textSecondary)
            }
            .frame(width: 36)

            // Status icon
            Image(systemName: statusIcon)
                .foregroundColor(statusColor)
                .font(.system(size: 18))

            // Detail
            VStack(alignment: .leading, spacing: 3) {
                Text(statusText)
                    .font(AppFont.buttonText())
                    .foregroundColor(Color.textPrimary)

                HStack(spacing: 8) {
                    if record.targetTime != "--:--" {
                        Label(record.targetTime, systemImage: "clock")
                            .font(AppFont.bodyText())
                            .foregroundColor(Color.textSecondary)
                    }
                    if record.windDownActivated {
                        Label("Wind Down", systemImage: "checkmark")
                            .font(AppFont.bodyText())
                            .foregroundColor(Color.accentStar)
                    }
                }
            }

            Spacer()
        }
        .padding(.vertical, AppLayout.cardPaddingV)
        .padding(.horizontal, AppLayout.cardPaddingH)
        .background(Color.backgroundSecondary)
        .cornerRadius(AppLayout.cardCornerRadius)
        .overlay(
            RoundedRectangle(cornerRadius: AppLayout.cardCornerRadius)
                .stroke(Color.cardBorder, lineWidth: AppLayout.cardBorderWidth)
        )
    }

    private var shortDate: String {
        let parts = record.date.split(separator: "-")
        guard parts.count == 3 else { return record.date }
        return "\(parts[1])/\(parts[2])"
    }

    private var statusIcon: String {
        switch record.choice {
        case .sleep: return "moon.fill"
        case .snooze: return "clock.arrow.circlepath"
        case .ignored: return "moon"
        case .pending: return "circle"
        }
    }

    private var statusColor: Color {
        switch record.choice {
        case .sleep: return Color.accentStar
        case .snooze: return Color.foxOrange
        case .ignored: return Color.textHint
        case .pending: return Color.textHint
        }
    }

    private var statusText: String {
        switch record.choice {
        case .sleep: return "準時準備入睡 ✨"
        case .snooze: return "延後了一下"
        case .ignored: return "沒有回應通知"
        case .pending: return "尚未到達提醒時間"
        }
    }
}
