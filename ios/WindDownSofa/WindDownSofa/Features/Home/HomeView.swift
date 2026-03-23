import SwiftUI

// MARK: - HomeView (PRD §3.1)

struct HomeView: View {
    @Binding var showWindDown: Bool
    @StateObject private var vm = HomeViewModel()

    @State private var cardOffset: CGFloat = 20
    @State private var cardOpacity: Double = 0

    var body: some View {
        ZStack {
            Color.backgroundPrimary.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 14) {
                    // App title + Fox
                    headerSection
                        .padding(.top, 16)

                    // Method carousel (transparent nav card style)
                    MethodCarouselView(
                        selectedMethodId: Binding(
                            get: { vm.selectedMethodId },
                            set: { vm.updateSelectedMethod($0) }
                        ),
                        methods: MethodRegistry.shared.allMethods
                    )
                    .padding(.horizontal, 16)

                    // Cards grid
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 14) {
                        SleepTimeCardView(
                            targetTimeString: Binding(
                                get: { vm.targetTimeString },
                                set: { vm.updateTargetTime($0) }
                            ),
                            onTimeChanged: { vm.updateTargetTime($0) }
                        )

                        ReminderTimeCardView(
                            targetTimeString: vm.targetTimeString,
                            leadMinutes: vm.leadMinutes
                        )
                    }
                    .padding(.horizontal, 16)

                    StreakCardView(streak: vm.streak)
                        .padding(.horizontal, 16)

                    // Primary CTA Button
                    Button {
                        showWindDown = true
                        StorageManager.shared.recordUserChoice(.sleep)
                    } label: {
                        Label("開始睡前儀式", systemImage: "moon.zzz.fill")
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .padding(.horizontal, 16)
                    .padding(.top, 4)
                    .padding(.bottom, 32)
                }
                .offset(y: cardOffset)
                .opacity(cardOpacity)
            }
        }
        .onAppear { animateIn() }
    }

    // MARK: - Header Section

    private var headerSection: some View {
        VStack(spacing: 4) {
            FoxMascotView(state: .drowsy)
                .frame(height: UIScreen.main.bounds.height * 0.28)
                .allowsHitTesting(false)

            Text("Wind Down Sofa")
                .font(AppFont.appName())
                .foregroundColor(Color.textPrimary)

            Text(greetingText)
                .font(AppFont.subtitle())
                .foregroundColor(Color.textSecondary)
        }
    }

    private var greetingText: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<12: return "早安，今天也好好照顧自己"
        case 12..<18: return "下午好，別忘了今晚的計劃"
        case 18..<22: return "晚上好，準備好放鬆了嗎？"
        default: return "夜深了，讓我們慢慢放鬆吧"
        }
    }

    private func animateIn() {
        withAnimation(AppAnimation.cardAppear) {
            cardOffset = 0
            cardOpacity = 1
        }
    }
}
