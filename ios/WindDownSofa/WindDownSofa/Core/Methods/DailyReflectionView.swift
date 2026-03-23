import SwiftUI

// MARK: - Daily Reflection Method (PRD §3.5)

struct DailyReflectionView: View {
    @StateObject private var vm = DailyReflectionViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                ForEach(Array(vm.questions.enumerated()), id: \.offset) { index, question in
                    ReflectionQuestionCard(
                        index: index + 1,
                        question: question,
                        answer: $vm.answers[index]
                    )
                }

                if vm.isSaved {
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(Color.accentStar)
                        Text("已記錄今晚的反思 ✨")
                            .font(AppFont.bodyText())
                            .foregroundColor(Color.textSecondary)
                    }
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                } else {
                    Button("儲存今日反思") {
                        withAnimation(AppAnimation.cardAppear) {
                            vm.save()
                        }
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(vm.answers.allSatisfy { $0.isEmpty })
                }
            }
            .padding(.horizontal, AppLayout.cardPaddingH)
            .padding(.vertical, 16)
        }
    }
}

// MARK: - Question Card

private struct ReflectionQuestionCard: View {
    let index: Int
    let question: String
    @Binding var answer: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Q\(index)  \(question)")
                .font(AppFont.bodyText())
                .foregroundColor(Color.textSecondary)

            TextEditor(text: $answer)
                .font(AppFont.bodyText())
                .foregroundColor(Color.textPrimary)
                .scrollContentBackground(.hidden)
                .background(Color.backgroundTertiary)
                .cornerRadius(12)
                .frame(minHeight: 72)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.cardBorder, lineWidth: 0.5)
                )
        }
    }
}

// MARK: - ViewModel

final class DailyReflectionViewModel: ObservableObject {
    let questions = [
        "今天讓你感到開心的一件事是什麼？",
        "今天有什麼挑戰？你是怎麼應對的？",
        "明天你最期待的事情是什麼？"
    ]

    @Published var answers: [String] = ["", "", ""]
    @Published var isSaved = false

    private let storageKey = "daily_reflection_"

    init() {
        loadTodayAnswers()
    }

    func save() {
        let today = DateFormatter.dateOnly.string(from: Date())
        let data = try? JSONEncoder().encode(answers)
        UserDefaults.standard.set(data, forKey: storageKey + today)
        isSaved = true
    }

    private func loadTodayAnswers() {
        let today = DateFormatter.dateOnly.string(from: Date())
        guard let data = UserDefaults.standard.data(forKey: storageKey + today),
              let saved = try? JSONDecoder().decode([String].self, from: data)
        else { return }
        answers = saved
        isSaved = !saved.allSatisfy { $0.isEmpty }
    }
}
