import SwiftUI

// MARK: - 4-7-8 Breathing Method (PRD §3.5)

struct Breathing478View: View {
    @StateObject private var vm = Breathing478ViewModel()

    var body: some View {
        VStack(spacing: 32) {
            Text(vm.phaseLabel)
                .font(AppFont.timeDisplayLarge())
                .foregroundColor(Color.textPrimary)
                .animation(AppAnimation.screenTransition, value: vm.phaseLabel)

            ZStack {
                Circle()
                    .fill(Color.accentPurple.opacity(0.25))
                    .frame(width: vm.circleSize, height: vm.circleSize)
                    .animation(.easeInOut(duration: vm.phaseDuration), value: vm.circleSize)

                Circle()
                    .fill(Color.accentPurple.opacity(0.5))
                    .frame(width: vm.circleSize * 0.65, height: vm.circleSize * 0.65)
                    .animation(.easeInOut(duration: vm.phaseDuration), value: vm.circleSize)

                Text("\(vm.countdown)")
                    .font(AppFont.timeDisplay())
                    .foregroundColor(Color.textPrimary)
            }
            .frame(width: 220, height: 220)

            Text(vm.instruction)
                .font(AppFont.bodyText())
                .foregroundColor(Color.textSecondary)
                .multilineTextAlignment(.center)

            if vm.cyclesCompleted > 0 {
                Text("已完成 \(vm.cyclesCompleted) 個循環")
                    .font(AppFont.cardLabel())
                    .foregroundColor(Color.textHint)
                    .tracking(1.5)
            }

            Button(vm.isRunning ? "暫停" : "開始") {
                vm.toggleRunning()
            }
            .buttonStyle(PrimaryButtonStyle())
            .padding(.horizontal, 40)
        }
        .padding(.vertical, 24)
        .onDisappear { vm.stop() }
    }
}

// MARK: - ViewModel

final class Breathing478ViewModel: ObservableObject {
    enum Phase: CaseIterable {
        case inhale, hold, exhale

        var duration: Double {
            switch self {
            case .inhale: return 4
            case .hold: return 7
            case .exhale: return 8
            }
        }

        var label: String {
            switch self {
            case .inhale: return "吸氣"
            case .hold: return "屏息"
            case .exhale: return "吐氣"
            }
        }

        var instruction: String {
            switch self {
            case .inhale: return "緩慢地用鼻子吸氣"
            case .hold: return "屏住呼吸，放鬆肩膀"
            case .exhale: return "用嘴巴緩緩吐氣"
            }
        }

        var targetCircleSize: CGFloat {
            switch self {
            case .inhale: return 180
            case .hold: return 180
            case .exhale: return 100
            }
        }
    }

    @Published var phase: Phase = .inhale
    @Published var countdown: Int = 4
    @Published var circleSize: CGFloat = 100
    @Published var isRunning = false
    @Published var cyclesCompleted = 0

    var phaseLabel: String { phase.label }
    var instruction: String { phase.instruction }
    var phaseDuration: Double { phase.duration }

    private var timer: Timer?
    private var secondsElapsed: Double = 0

    func toggleRunning() {
        if isRunning { stop() } else { start() }
    }

    func start() {
        isRunning = true
        circleSize = phase.targetCircleSize
        tick()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.tick()
        }
    }

    func stop() {
        isRunning = false
        timer?.invalidate()
        timer = nil
    }

    private func tick() {
        let remaining = Int(phase.duration) - Int(secondsElapsed)
        countdown = max(remaining, 0)

        if secondsElapsed >= phase.duration {
            advancePhase()
        }
        secondsElapsed += 1
    }

    private func advancePhase() {
        secondsElapsed = 0
        let phases = Phase.allCases
        let currentIndex = phases.firstIndex(of: phase) ?? 0
        let nextIndex = (currentIndex + 1) % phases.count

        if nextIndex == 0 { cyclesCompleted += 1 }
        phase = phases[nextIndex]

        withAnimation(.easeInOut(duration: phase.duration)) {
            circleSize = phase.targetCircleSize
        }
        countdown = Int(phase.duration)
    }
}
