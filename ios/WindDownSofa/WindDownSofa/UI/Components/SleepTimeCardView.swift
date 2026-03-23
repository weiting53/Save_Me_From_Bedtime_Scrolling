import SwiftUI

// MARK: - SleepTimeCardView (PRD §3.1)

struct SleepTimeCardView: View {
    @Binding var targetTimeString: String
    var onTimeChanged: ((String) -> Void)?

    @State private var showPicker = false
    @State private var pickerDate: Date = Date()

    var body: some View {
        Button {
            preparePicker()
            showPicker = true
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                Text("目標睡眠時間")
                    .cardLabel()

                HStack(alignment: .firstTextBaseline) {
                    Text(targetTimeString)
                        .font(AppFont.timeDisplay())
                        .foregroundColor(Color.textPrimary)

                    Spacer()

                    Image(systemName: "pencil")
                        .foregroundColor(Color.textSecondary)
                        .font(.system(size: 16))
                }
            }
        }
        .buttonStyle(.plain)
        .cardStyle()
        .sheet(isPresented: $showPicker) {
            SleepTimePickerSheet(
                pickerDate: $pickerDate,
                onSave: { date in
                    let timeString = DateFormatter.timeOnly.string(from: date)
                    targetTimeString = timeString
                    onTimeChanged?(timeString)
                    showPicker = false
                },
                onCancel: { showPicker = false }
            )
            .presentationDetents([.medium])
        }
    }

    private func preparePicker() {
        let components = targetTimeString.split(separator: ":").compactMap { Int($0) }
        guard components.count == 2 else { return }
        var dc = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        dc.hour = components[0]
        dc.minute = components[1]
        pickerDate = Calendar.current.date(from: dc) ?? Date()
    }
}

// MARK: - Picker Sheet

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
