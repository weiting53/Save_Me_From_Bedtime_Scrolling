import SwiftUI

// MARK: - StreakCardView (PRD §3.1)

struct StreakCardView: View {
    let streak: Int

    @State private var displayStreak: Int = 0
    @State private var appeared = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("連續達成")
                .cardLabel()

            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("\(displayStreak)")
                    .font(AppFont.timeDisplay())
                    .foregroundColor(streak > 0 ? Color.accentStar : Color.textSecondary)
                    .animation(AppAnimation.streak, value: displayStreak)

                Text("天")
                    .font(AppFont.bodyText())
                    .foregroundColor(Color.textSecondary)

                Spacer()

                streakIcon
            }

            Text(motivationText)
                .font(AppFont.bodyText())
                .foregroundColor(Color.textSecondary)
                .lineSpacing(4)
        }
        .cardStyle()
        .onAppear {
            if !appeared {
                appeared = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                    withAnimation(AppAnimation.streak) {
                        displayStreak = streak
                    }
                }
            }
        }
        .onChange(of: streak) { newValue in
            withAnimation(AppAnimation.streak) {
                displayStreak = newValue
            }
        }
    }

    private var streakIcon: some View {
        Group {
            if streak >= 7 {
                Image(systemName: "flame.fill")
                    .foregroundColor(.orange)
            } else if streak >= 3 {
                Image(systemName: "star.fill")
                    .foregroundColor(Color.accentStar)
            } else {
                Image(systemName: "moon.stars")
                    .foregroundColor(Color.textSecondary)
            }
        }
        .font(.system(size: 20))
    }

    private var motivationText: String {
        switch streak {
        case 0:
            return "今晚是新的開始，你可以的 🌙"
        case 1:
            return "第一步最難，你已經邁出去了 ✨"
        case 2...6:
            return "繼續保持，你的身體正在感謝你 💫"
        case 7...13:
            return "一週了！你正在建立真正的習慣 🌟"
        default:
            return "你已經是自己最棒的夢想守護者 🦊"
        }
    }
}
