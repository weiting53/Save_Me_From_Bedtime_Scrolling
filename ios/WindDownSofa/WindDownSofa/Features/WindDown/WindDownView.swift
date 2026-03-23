import SwiftUI

// MARK: - WindDownView (PRD §3.2)

struct WindDownView: View {
    @Binding var isPresented: Bool
    @StateObject private var vm = WindDownViewModel()

    var body: some View {
        ZStack {
            Color.backgroundPrimary.ignoresSafeArea()

            if !vm.isEntranceComplete {
                entranceScreen
            } else {
                mainContent
                    .opacity(vm.contentOpacity)
                    .transition(.opacity)
            }
        }
        .onAppear {
            vm.startEntrance()
        }
        .sheet(isPresented: $vm.showShortcutsSetup) {
            ShortcutsSetupSheet()
                .presentationDetents([.medium, .large])
        }
    }

    // MARK: - Entrance (1.5s, 不可跳過 per PRD §3.2)

    private var entranceScreen: some View {
        VStack(spacing: 28) {
            Spacer()

            FoxMascotView(state: .sleeping)
                .frame(height: 200)

            Image(systemName: "moon.stars.fill")
                .font(.system(size: 48))
                .foregroundColor(Color.accentStar)
                .scaleEffect(vm.moonScale)
                .opacity(vm.moonOpacity)

            Text("準備好了，讓我們慢慢放鬆吧")
                .font(AppFont.subtitle())
                .foregroundColor(Color.textSecondary)
                .opacity(vm.moonOpacity)

            Spacer()
        }
    }

    // MARK: - Main Content

    private var mainContent: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Wind Down")
                            .font(AppFont.appName())
                            .foregroundColor(Color.textPrimary)
                        Text(vm.selectedMethod.name)
                            .font(AppFont.subtitle())
                            .foregroundColor(Color.textSecondary)
                    }

                    Spacer()

                    Button {
                        isPresented = false
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24))
                            .foregroundColor(Color.textHint)
                    }
                }
                .padding(.horizontal, AppLayout.cardPaddingH)
                .padding(.top, 56)

                // Fox (small)
                FoxMascotView(state: .sleeping)
                    .frame(height: 120)

                // Method View
                vm.selectedMethod.view()
                    .padding(.horizontal, 4)

                // Shortcuts Card
                ShortcutsLaunchCard(onLaunch: { vm.launchShortcuts() })
                    .padding(.horizontal, 16)
                    .padding(.bottom, 40)
            }
        }
    }
}

// MARK: - Shortcuts Launch Card

private struct ShortcutsLaunchCard: View {
    let onLaunch: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: "square.grid.2x2.fill")
                    .foregroundColor(Color.accentStar)
                    .font(.system(size: 18))

                VStack(alignment: .leading, spacing: 2) {
                    Text("啟動 Wind Down 捷徑")
                        .font(AppFont.buttonText())
                        .foregroundColor(Color.textPrimary)

                    Text("自動調暗螢幕、開啟勿擾模式")
                        .font(AppFont.bodyText())
                        .foregroundColor(Color.textSecondary)
                }
            }

            Button("啟動 Wind Down", action: onLaunch)
                .buttonStyle(PrimaryButtonStyle())
        }
        .cardStyle()
    }
}

// MARK: - Shortcuts Setup Sheet

private struct ShortcutsSetupSheet: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Color.backgroundPrimary.ignoresSafeArea()

            VStack(spacing: 24) {
                Text("設定 Wind Down 捷徑")
                    .font(AppFont.appName())
                    .foregroundColor(Color.textPrimary)
                    .padding(.top, 32)

                VStack(alignment: .leading, spacing: 16) {
                    SetupStep(
                        number: "1",
                        title: "開啟 Shortcuts App",
                        description: "在你的 iPhone 上找到「捷徑」App"
                    )
                    SetupStep(
                        number: "2",
                        title: "新增捷徑",
                        description: "點擊右上角「+」，命名為「WindDown」"
                    )
                    SetupStep(
                        number: "3",
                        title: "加入動作",
                        description: "加入「設定外觀」（深色模式）和「設定勿擾模式」"
                    )
                }
                .padding(.horizontal, 24)

                Spacer()

                Button("我了解了") { dismiss() }
                    .buttonStyle(PrimaryButtonStyle())
                    .padding(.horizontal, 24)
                    .padding(.bottom, 32)
            }
        }
    }
}

private struct SetupStep: View {
    let number: String
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            Text(number)
                .font(AppFont.buttonText())
                .foregroundColor(Color.backgroundPrimary)
                .frame(width: 28, height: 28)
                .background(Color.accentPurple)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(AppFont.buttonText())
                    .foregroundColor(Color.textPrimary)

                Text(description)
                    .font(AppFont.bodyText())
                    .foregroundColor(Color.textSecondary)
            }
        }
    }
}
