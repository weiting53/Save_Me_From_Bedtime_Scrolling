import SwiftUI
import UserNotifications

// MARK: - SettingsView (PRD §3.4)

struct SettingsView: View {
    @StateObject private var vm = SettingsViewModel()
    @State private var pickerDate: Date = Date()
    @State private var showTimePicker = false

    var body: some View {
        ZStack {
            Color.backgroundPrimary.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("設定")
                        .font(AppFont.appName())
                        .foregroundColor(Color.textPrimary)
                        .padding(.horizontal, AppLayout.cardPaddingH)
                        .padding(.top, 60)

                    // Notification section
                    settingsSection(title: "通知") {
                        notificationStatusRow
                        leadTimeRow
                    }

                    // Sleep time section
                    settingsSection(title: "睡眠時間") {
                        defaultSleepTimeRow
                    }

                    // Data section
                    settingsSection(title: "資料") {
                        resetDataRow
                    }

                    // Version info
                    Text("Wind Down Sofa v1.0 · Made with 🦊")
                        .font(AppFont.bodyText())
                        .foregroundColor(Color.textHint)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 8)
                        .padding(.bottom, 40)
                }
            }
        }
        .onAppear { vm.refreshNotificationStatus() }
        .sheet(isPresented: $showTimePicker) {
            SleepTimePickerSheet(
                pickerDate: $pickerDate,
                onSave: { date in
                    vm.updateDefaultSleepTime(DateFormatter.timeOnly.string(from: date))
                    showTimePicker = false
                },
                onCancel: { showTimePicker = false }
            )
            .presentationDetents([.medium])
        }
        .alert("確定要重置所有資料嗎？", isPresented: $vm.showResetConfirm) {
            Button("重置", role: .destructive) { vm.resetAllData() }
            Button("取消", role: .cancel) {}
        } message: {
            Text("這個操作無法復原，所有歷史記錄與 Streak 都會被清除。")
        }
    }

    // MARK: - Section Builder

    private func settingsSection<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .cardLabel()
                .padding(.horizontal, AppLayout.cardPaddingH + 4)

            VStack(spacing: 0) {
                content()
            }
            .background(Color.backgroundSecondary)
            .cornerRadius(AppLayout.cardCornerRadius)
            .overlay(
                RoundedRectangle(cornerRadius: AppLayout.cardCornerRadius)
                    .stroke(Color.cardBorder, lineWidth: AppLayout.cardBorderWidth)
            )
            .padding(.horizontal, 16)
        }
    }

    // MARK: - Rows

    private var notificationStatusRow: some View {
        HStack {
            Label("通知權限", systemImage: "bell.fill")
                .font(AppFont.buttonText())
                .foregroundColor(Color.textPrimary)

            Spacer()

            if vm.notificationStatus == .authorized {
                Label("已開啟", systemImage: "checkmark.circle.fill")
                    .font(AppFont.bodyText())
                    .foregroundColor(Color.accentStar)
            } else {
                Button("前往設定") {
                    vm.openSystemSettings()
                }
                .font(AppFont.bodyText())
                .foregroundColor(Color.foxOrange)
            }
        }
        .padding(.vertical, AppLayout.cardPaddingV)
        .padding(.horizontal, AppLayout.cardPaddingH)
        .overlay(alignment: .bottom) {
            Divider().background(Color.cardBorder)
        }
    }

    private var leadTimeRow: some View {
        VStack(alignment: .leading, spacing: 10) {
            Label("提前提醒時間", systemImage: "clock.arrow.2.circlepath")
                .font(AppFont.buttonText())
                .foregroundColor(Color.textPrimary)

            HStack(spacing: 8) {
                ForEach(vm.leadMinuteOptions, id: \.self) { minutes in
                    Button("\(minutes) 分") {
                        vm.updateLeadMinutes(minutes)
                    }
                    .font(AppFont.bodyText())
                    .foregroundColor(
                        vm.reminderLeadMinutes == minutes ? Color.textPrimary : Color.textSecondary
                    )
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(
                        vm.reminderLeadMinutes == minutes ? Color.accentPurple : Color.backgroundTertiary
                    )
                    .cornerRadius(20)
                }
            }
        }
        .padding(.vertical, AppLayout.cardPaddingV)
        .padding(.horizontal, AppLayout.cardPaddingH)
    }

    private var defaultSleepTimeRow: some View {
        Button {
            prepareTimePicker()
            showTimePicker = true
        } label: {
            HStack {
                Label("預設目標時間", systemImage: "moon.fill")
                    .font(AppFont.buttonText())
                    .foregroundColor(Color.textPrimary)

                Spacer()

                Text(vm.defaultSleepTime)
                    .font(AppFont.bodyText())
                    .foregroundColor(Color.textSecondary)

                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundColor(Color.textHint)
            }
            .padding(.vertical, AppLayout.cardPaddingV)
            .padding(.horizontal, AppLayout.cardPaddingH)
        }
        .buttonStyle(.plain)
    }

    private var resetDataRow: some View {
        Button {
            vm.showResetConfirm = true
        } label: {
            HStack {
                Label("重置所有資料", systemImage: "trash")
                    .font(AppFont.buttonText())
                    .foregroundColor(Color.textSecondary)

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundColor(Color.textHint)
            }
            .padding(.vertical, AppLayout.cardPaddingV)
            .padding(.horizontal, AppLayout.cardPaddingH)
        }
        .buttonStyle(.plain)
    }

    private func prepareTimePicker() {
        let components = vm.defaultSleepTime.split(separator: ":").compactMap { Int($0) }
        guard components.count == 2 else { return }
        var dc = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        dc.hour = components[0]
        dc.minute = components[1]
        pickerDate = Calendar.current.date(from: dc) ?? Date()
    }
}

// MARK: - Reuse SleepTimePickerSheet from SleepTimeCardView

private struct SleepTimePickerSheet: View {
    @Binding var pickerDate: Date
    let onSave: (Date) -> Void
    let onCancel: () -> Void

    var body: some View {
        ZStack {
            Color.backgroundPrimary.ignoresSafeArea()

            VStack(spacing: 24) {
                Text("設定目標睡眠時間")
                    .font(AppFont.appName())
                    .foregroundColor(Color.textPrimary)
                    .padding(.top, 24)

                DatePicker("", selection: $pickerDate, displayedComponents: .hourAndMinute)
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                    .colorScheme(.dark)

                HStack(spacing: 16) {
                    Button("取消", action: onCancel)
                        .font(AppFont.buttonText())
                        .foregroundColor(Color.textSecondary)
                        .frame(maxWidth: .infinity)
                        .frame(height: AppLayout.buttonHeight)
                        .overlay(
                            RoundedRectangle(cornerRadius: AppLayout.buttonCornerRadius)
                                .stroke(Color.cardBorder)
                        )

                    Button("儲存") { onSave(pickerDate) }
                        .buttonStyle(PrimaryButtonStyle())
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
    }
}
