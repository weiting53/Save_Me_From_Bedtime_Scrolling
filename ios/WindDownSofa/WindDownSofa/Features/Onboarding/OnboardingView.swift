import SwiftUI

// MARK: - OnboardingView (PRD §2.2)

struct OnboardingView: View {
    @StateObject private var vm = OnboardingViewModel()

    var body: some View {
        ZStack {
            Color.backgroundPrimary.ignoresSafeArea()

            VStack(spacing: 0) {
                TabView(selection: $vm.currentPage) {
                    OnboardingPageView(
                        foxState: .lively,
                        title: "歡迎來到 Wind Down Sofa",
                        subtitle: "你的溫柔睡前陪伴者",
                        body: "我們不強迫你，只是在你快要破功的時刻，輕輕問你一句話。"
                    )
                    .tag(0)

                    OnboardingPageView(
                        foxState: .drowsy,
                        title: "它是怎麼運作的？",
                        subtitle: "三個簡單步驟",
                        body: "1. 設定今晚目標睡眠時間\n2. 到時間前 30 分鐘收到通知\n3. 選擇準備睡了，或再給自己 15 分鐘"
                    )
                    .tag(1)

                    OnboardingPageView(
                        foxState: .lively,
                        title: "記錄你的模式",
                        subtitle: "越來越了解自己",
                        body: "每天的選擇都會被記錄下來，幫助你看見自己的睡眠習慣，慢慢建立 Streak。"
                    )
                    .tag(2)

                    OnboardingNotificationPage(vm: vm)
                        .tag(3)

                    OnboardingSetupPage(vm: vm)
                        .tag(4)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(AppAnimation.screenTransition, value: vm.currentPage)

                OnboardingBottomBar(vm: vm)
                    .padding(.bottom, 32)
            }
        }
    }
}

// MARK: - Page Views

private struct OnboardingPageView: View {
    let foxState: FoxState
    let title: String
    let subtitle: String
    let body: String

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            FoxMascotView(state: foxState)
                .frame(height: UIScreen.main.bounds.height * 0.28)

            VStack(spacing: 12) {
                Text(title)
                    .font(AppFont.appName())
                    .foregroundColor(Color.textPrimary)
                    .multilineTextAlignment(.center)

                Text(subtitle)
                    .font(AppFont.subtitle())
                    .foregroundColor(Color.textSecondary)

                Text(body)
                    .font(AppFont.bodyText())
                    .foregroundColor(Color.textSecondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(6)
                    .padding(.horizontal, 32)
            }

            Spacer()
        }
    }
}

private struct OnboardingNotificationPage: View {
    @ObservedObject var vm: OnboardingViewModel

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            FoxMascotView(state: .lively)
                .frame(height: UIScreen.main.bounds.height * 0.25)

            VStack(spacing: 12) {
                Text("開啟通知")
                    .font(AppFont.appName())
                    .foregroundColor(Color.textPrimary)

                Text("才能在對的時刻提醒你")
                    .font(AppFont.subtitle())
                    .foregroundColor(Color.textSecondary)

                Text("Wind Down Sofa 只會在你設定的睡前時間附近發送通知，不會打擾你其他時間。")
                    .font(AppFont.bodyText())
                    .foregroundColor(Color.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                    .lineSpacing(6)
            }

            Spacer()

            Button("開啟通知權限") {
                vm.requestNotifications()
            }
            .buttonStyle(PrimaryButtonStyle())
            .padding(.horizontal, 32)

            Button("稍後再說") {
                vm.nextPage()
            }
            .font(AppFont.bodyText())
            .foregroundColor(Color.textSecondary)
            .padding(.bottom, 8)
        }
    }
}

private struct OnboardingSetupPage: View {
    @ObservedObject var vm: OnboardingViewModel

    var body: some View {
        VStack(spacing: 28) {
            Spacer()

            VStack(spacing: 8) {
                Text("設定今晚的目標")
                    .font(AppFont.appName())
                    .foregroundColor(Color.textPrimary)

                Text("你想幾點睡著？")
                    .font(AppFont.subtitle())
                    .foregroundColor(Color.textSecondary)
            }

            DatePicker(
                "",
                selection: $vm.targetSleepTime,
                displayedComponents: .hourAndMinute
            )
            .datePickerStyle(.wheel)
            .labelsHidden()
            .colorScheme(.dark)
            .frame(maxWidth: .infinity)

            VStack(alignment: .leading, spacing: 12) {
                Text("選擇你的第一個助眠方法")
                    .cardLabel()
                    .padding(.horizontal, 8)

                ForEach(MethodRegistry.shared.allMethods) { method in
                    MethodSelectionRow(
                        method: method,
                        isSelected: vm.selectedMethodId == method.id
                    ) {
                        vm.selectedMethodId = method.id
                    }
                }
            }
            .padding(.horizontal, 24)

            Spacer()

            Button("開始使用") {
                vm.finish()
            }
            .buttonStyle(PrimaryButtonStyle())
            .padding(.horizontal, 32)
        }
    }
}

private struct MethodSelectionRow: View {
    let method: AnyWindDownMethod
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 14) {
                Image(systemName: method.icon)
                    .foregroundColor(isSelected ? Color.accentStar : Color.textSecondary)
                    .frame(width: 24)

                VStack(alignment: .leading, spacing: 2) {
                    Text(method.name)
                        .font(AppFont.buttonText())
                        .foregroundColor(Color.textPrimary)
                    Text(method.description)
                        .font(AppFont.bodyText())
                        .foregroundColor(Color.textSecondary)
                        .lineLimit(2)
                }

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(Color.accentStar)
                }
            }
            .padding(.vertical, AppLayout.cardPaddingV)
            .padding(.horizontal, AppLayout.cardPaddingH)
            .background(isSelected ? Color.backgroundTertiary : Color.backgroundSecondary)
            .cornerRadius(AppLayout.cardCornerRadius)
            .overlay(
                RoundedRectangle(cornerRadius: AppLayout.cardCornerRadius)
                    .stroke(
                        isSelected ? Color.navBorder : Color.cardBorder,
                        lineWidth: isSelected ? AppLayout.navBorderWidth : AppLayout.cardBorderWidth
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Bottom Navigation Bar

private struct OnboardingBottomBar: View {
    @ObservedObject var vm: OnboardingViewModel

    var body: some View {
        HStack {
            PageIndicatorView(
                currentPage: vm.currentPage,
                totalPages: vm.totalPages
            )

            Spacer()

            if vm.currentPage < vm.totalPages - 1 && vm.currentPage != 3 {
                Button("下一步") {
                    vm.nextPage()
                }
                .font(AppFont.buttonText())
                .foregroundColor(Color.textPrimary)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(Color.accentPurple)
                .cornerRadius(AppLayout.buttonCornerRadius)
            }
        }
        .padding(.horizontal, 32)
    }
}

// MARK: - Page Indicator

struct PageIndicatorView: View {
    let currentPage: Int
    let totalPages: Int

    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<totalPages, id: \.self) { index in
                Circle()
                    .fill(index == currentPage ? Color.accentStar : Color.textHint)
                    .frame(
                        width: index == currentPage ? AppLayout.dotSizeSelected : AppLayout.dotSizeUnselected,
                        height: index == currentPage ? AppLayout.dotSizeSelected : AppLayout.dotSizeUnselected
                    )
                    .animation(.spring(response: 0.3), value: currentPage)
            }
        }
    }
}
