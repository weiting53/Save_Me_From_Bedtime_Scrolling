import SwiftUI

// MARK: - App State

final class AppState: ObservableObject {
    @Published var bedtimeHour:   Int  = 11
    @Published var bedtimeMinute: Int  = 30
    @Published var shortcutInstalled: Bool = false
    @Published var onboardingDone: Bool = false

    var bedtimeFormatted: String {
        let h = bedtimeHour == 0 ? 12 : (bedtimeHour > 12 ? bedtimeHour - 12 : bedtimeHour)
        let period = bedtimeHour < 12 ? "AM" : "PM"
        return String(format: "%d:%02d %@", h, bedtimeMinute, period)
    }

    // Reminder = 15 min before bedtime
    var reminderHour: Int {
        let total = bedtimeHour * 60 + bedtimeMinute - 15
        return ((total / 60) + 24) % 24
    }
    var reminderMinute: Int {
        ((bedtimeHour * 60 + bedtimeMinute - 15) % 60 + 60) % 60
    }
    var reminderFormatted: String {
        let h = reminderHour == 0 ? 12 : (reminderHour > 12 ? reminderHour - 12 : reminderHour)
        let period = reminderHour < 12 ? "AM" : "PM"
        return String(format: "%d:%02d %@", h, reminderMinute, period)
    }
}

// MARK: - Root view

struct ContentView: View {
    @StateObject private var state = AppState()

    var body: some View {
        Group {
            if state.onboardingDone {
                MainView()
                    .environmentObject(state)
            } else {
                OnboardingView()
                    .environmentObject(state)
            }
        }
        .preferredColorScheme(.dark)
    }
}

// MARK: - Star model

private struct Star {
    var x, y, r, phase, speed: Double
}

private struct Meteor: Identifiable {
    let id = UUID()
    var x, y, alpha: Double
    let length, speed: Double
    let angle: Double = .pi / 5.5
}

// MARK: - Star field canvas

struct StarFieldView: View {
    @State private var stars:  [Star]   = []
    @State private var meteors:[Meteor] = []
    @State private var t: Double = 0
    private let fps = 1.0 / 60.0

    var body: some View {
        TimelineView(.animation) { tl in
            Canvas { ctx, size in
                // Background
                ctx.fill(Path(CGRect(origin: .zero, size: size)),
                         with: .color(Color(red: 0.02, green: 0.04, blue: 0.12)))

                // Stars
                for s in stars {
                    let a = 0.25 + 0.6 * sin(s.phase + t * s.speed)
                    let r = CGFloat(s.r)
                    ctx.fill(
                        Path(ellipseIn: CGRect(x: s.x - s.r, y: s.y - s.r,
                                               width: s.r * 2, height: s.r * 2)),
                        with: .color(Color(red: 0.87, green: 0.85, blue: 1).opacity(max(0, a)))
                    )
                    _ = r
                }

                // Meteors
                for m in meteors {
                    var p = Path()
                    p.move(to:   CGPoint(x: m.x, y: m.y))
                    p.addLine(to: CGPoint(x: m.x + cos(m.angle) * m.length,
                                          y: m.y + sin(m.angle) * m.length))
                    ctx.stroke(p,
                               with: .color(Color.white.opacity(m.alpha * 0.7)),
                               style: StrokeStyle(lineWidth: 1.2, lineCap: .round))
                }
            }
            .onChange(of: tl.date) { _ in tick() }
        }
        .onAppear { setup() }
    }

    private func setup() {
        stars = (0..<160).map { _ in
            Star(x:     Double.random(in: 0...420),
                 y:     Double.random(in: 0...900),
                 r:     Double.random(in: 0.3...1.4),
                 phase: Double.random(in: 0...(2 * .pi)),
                 speed: Double.random(in: 0.001...0.006))
        }
        spawnMeteor()
    }

    private func spawnMeteor() {
        meteors.append(Meteor(
            x:      Double.random(in: 20...340),
            y:      Double.random(in: 10...180),
            alpha:  1,
            length: Double.random(in: 55...110),
            speed:  Double.random(in: 3...7)
        ))
    }

    private func tick() {
        t += fps
        meteors = meteors.compactMap {
            var m = $0
            m.x     += cos(m.angle) * m.speed
            m.y     += sin(m.angle) * m.speed
            m.alpha *= 0.974
            return m.alpha > 0.01 ? m : nil
        }
        // Spawn new meteor randomly
        if meteors.isEmpty || (meteors.allSatisfy { $0.alpha < 0.08 } && Double.random(in: 0...1) < 0.03) {
            spawnMeteor()
        }
    }
}

// MARK: - Onboarding view

struct OnboardingView: View {
    @EnvironmentObject private var state: AppState
    @State private var showPicker  = false
    @State private var showGuide   = false
    @State private var installPhase: InstallPhase = .idle

    enum InstallPhase { case idle, installing, done }

    var body: some View {
        ZStack {
            StarFieldView().ignoresSafeArea()

            // Bottom vignette
            VStack {
                Spacer()
                LinearGradient(
                    colors: [.clear, Color(red: 0.02, green: 0.04, blue: 0.12).opacity(0.85)],
                    startPoint: .top, endPoint: .bottom
                )
                .frame(height: 280)
                .ignoresSafeArea()
            }

            // Content
            VStack(spacing: 0) {
                Spacer()
                moonIcon
                    .padding(.bottom, 24)

                Text(L.bedtimeQuestion)
                    .font(.system(size: 26, weight: .light, design: .serif))
                    .foregroundColor(cream)
                    .multilineTextAlignment(.center)
                    .padding(.bottom, 8)

                Text(L.bedtimeSubtitle)
                    .font(.system(size: 13))
                    .foregroundColor(Color(white: 0.38))
                    .multilineTextAlignment(.center)
                    .padding(.bottom, 36)

                timeField
                    .padding(.horizontal, 32)
                    .padding(.bottom, 14)

                installButton
                    .padding(.horizontal, 32)

                Text(L.installHint)
                    .font(.system(size: 11))
                    .foregroundColor(Color(white: 0.20))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .padding(.top, 14)
                    .padding(.bottom, 52)
            }

            // Time picker overlay
            if showPicker {
                TimePickerView(
                    hour:   $state.bedtimeHour,
                    minute: $state.bedtimeMinute,
                    onConfirm: { showPicker = false }
                )
                .transition(.opacity.combined(with: .scale(scale: 0.98)))
            }

            // Setup guide overlay
            if showGuide {
                SetupGuideView(onDone: {
                    showGuide = false
                    state.onboardingDone = true
                })
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
        .preferredColorScheme(.dark)
        .animation(.easeInOut(duration: 0.3), value: showPicker)
        .animation(.easeInOut(duration: 0.4), value: showGuide)
    }

    // MARK: - Sub-views

    private var moonIcon: some View {
        ZStack {
            Circle()
                .fill(cream.opacity(0.85))
                .frame(width: 44, height: 44)
            Circle()
                .fill(Color(red: 0.04, green: 0.07, blue: 0.18))
                .frame(width: 34, height: 34)
                .offset(x: 8, y: -4)
        }
        .frame(width: 48, height: 48)
        .clipped()
    }

    private var timeField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L.bedtimeLabel.uppercased())
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(Color(white: 0.28))
                .kerning(1.4)

            Button { withAnimation { showPicker = true } } label: {
                HStack {
                    Text(state.bedtimeFormatted)
                        .font(.system(size: 34, weight: .thin, design: .serif))
                        .foregroundColor(cream)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .light))
                        .foregroundColor(Color(white: 0.28))
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 18)
                .background(
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color.white.opacity(0.04))
                        .overlay(RoundedRectangle(cornerRadius: 18)
                            .stroke(Color.white.opacity(0.09), lineWidth: 0.5))
                )
            }
        }
    }

    private var installButton: some View {
        Button { handleInstall() } label: {
            HStack(spacing: 8) {
                switch installPhase {
                case .idle:
                    Text(L.installBtn)
                case .installing:
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: lavender))
                        .scaleEffect(0.75)
                    Text(L.installingBtn)
                case .done:
                    Text(L.installedBtn)
                }
            }
            .font(.system(size: 15, weight: .medium))
            .foregroundColor(installPhase == .done ? mint : lavender)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill((installPhase == .done ? Color.green : Color.purple).opacity(0.12))
                    .overlay(RoundedRectangle(cornerRadius: 18)
                        .stroke((installPhase == .done ? mint : lavender).opacity(0.28), lineWidth: 0.5))
            )
        }
        .disabled(installPhase == .installing)
        .animation(.easeInOut(duration: 0.25), value: installPhase)
    }

    // MARK: - Actions

    private func handleInstall() {
        guard installPhase == .idle else { return }
        installPhase = .installing

        // Update the localised reminder time in L so the guide shows correct time
        L.reminderTimeString = state.reminderFormatted

        // Ask for notification permission
        Task {
            await NotificationManager.requestPermission()
            NotificationManager.scheduleMorningInsight(
                bedtimeHour:   state.bedtimeHour,
                bedtimeMinute: state.bedtimeMinute,
                overMinutes:   0
            )
        }

        // Generate + share the .shortcut file
        guard let fileURL = ShortcutGenerator.generate() else {
            installPhase = .idle
            return
        }

        // Share sheet so user can open it in Shortcuts
        let av = UIActivityViewController(activityItems: [fileURL], applicationActivities: nil)
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let root  = scene.windows.first?.rootViewController {
            av.completionWithItemsHandler = { _, completed, _, _ in
                DispatchQueue.main.async {
                    if completed {
                        withAnimation { installPhase = .done }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                            withAnimation { showGuide = true }
                        }
                    } else {
                        installPhase = .idle
                    }
                }
            }
            root.present(av, animated: true)
        }
    }

    // MARK: - Colours
    private let cream   = Color(red: 0.91, green: 0.87, blue: 0.78)
    private let lavender = Color(red: 0.78, green: 0.72, blue: 0.91)
    private let mint    = Color(red: 0.55, green: 0.85, blue: 0.68)
}

// MARK: - Time picker overlay

struct TimePickerView: View {
    @Binding var hour: Int
    @Binding var minute: Int
    var onConfirm: () -> Void

    private let cream    = Color(red: 0.91, green: 0.87, blue: 0.78)
    private let lavender = Color(red: 0.78, green: 0.72, blue: 0.91)

    var body: some View {
        ZStack {
            Color(red: 0.02, green: 0.04, blue: 0.12).opacity(0.97).ignoresSafeArea()

            VStack(spacing: 0) {
                Text(L.bedtimeQuestion)
                    .font(.system(size: 20, weight: .light, design: .serif))
                    .foregroundColor(cream)
                    .padding(.bottom, 40)

                HStack(alignment: .center, spacing: 20) {
                    SpinnerColumn(value: $hour,   min: 1,  max: 12, step: 1)
                    Text(":")
                        .font(.system(size: 50, weight: .thin, design: .serif))
                        .foregroundColor(Color(white: 0.22))
                        .padding(.bottom, 4)
                    SpinnerColumn(value: $minute, min: 0,  max: 55, step: 5, pad: true)

                    // AM/PM toggle
                    VStack(spacing: 12) {
                        Spacer().frame(height: 40)
                        Text("AM")
                            .font(.system(size: 13, weight: .light))
                            .foregroundColor(Color(white: 0.3))
                        Text("PM")
                            .font(.system(size: 13, weight: .light))
                            .foregroundColor(Color(white: 0.3))
                    }
                }
                .padding(.bottom, 48)

                Button(action: onConfirm) {
                    Text(L.isZh ? "確認" : "Confirm")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(lavender)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 18)
                        .background(
                            RoundedRectangle(cornerRadius: 18)
                                .fill(Color.purple.opacity(0.12))
                                .overlay(RoundedRectangle(cornerRadius: 18)
                                    .stroke(lavender.opacity(0.28), lineWidth: 0.5))
                        )
                }
                .padding(.horizontal, 32)
            }
        }
    }
}

// MARK: - Spinner column

struct SpinnerColumn: View {
    @Binding var value: Int
    let min, max, step: Int
    var pad: Bool = false
    private let cream = Color(red: 0.91, green: 0.87, blue: 0.78)

    var body: some View {
        VStack(spacing: 16) {
            button(systemName: "chevron.up")  { increment(+1) }

            Text(pad ? String(format: "%02d", value) : "\(value)")
                .font(.system(size: 50, weight: .thin, design: .serif))
                .foregroundColor(cream)
                .frame(minWidth: 64)
                .contentTransition(.numericText())
                .animation(.easeInOut(duration: 0.14), value: value)

            button(systemName: "chevron.down") { increment(-1) }
        }
    }

    private func button(systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 17, weight: .light))
                .foregroundColor(Color(white: 0.28))
                .padding(10)
        }
    }

    private func increment(_ dir: Int) {
        let next = value + dir * step
        if next > max { value = min }
        else if next < min { value = max }
        else { value = next }
    }
}

// MARK: - Setup guide overlay

struct SetupGuideView: View {
    var onDone: () -> Void
    private let cream    = Color(red: 0.91, green: 0.87, blue: 0.78)
    private let lavender = Color(red: 0.78, green: 0.72, blue: 0.91)

    var body: some View {
        ZStack {
            Color(red: 0.02, green: 0.04, blue: 0.12).opacity(0.97).ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                // Icon
                Image(systemName: "checklist")
                    .font(.system(size: 36, weight: .thin))
                    .foregroundColor(lavender.opacity(0.7))
                    .padding(.bottom, 28)

                Text(L.guideTitle)
                    .font(.system(size: 24, weight: .light, design: .serif))
                    .foregroundColor(cream)
                    .padding(.bottom, 24)

                Text(L.guideBody)
                    .font(.system(size: 15, weight: .light))
                    .foregroundColor(Color(white: 0.5))
                    .multilineTextAlignment(.center)
                    .lineSpacing(5)
                    .padding(.horizontal, 36)

                Spacer()

                Button(action: onDone) {
                    Text(L.guideDone)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(lavender)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 18)
                        .background(
                            RoundedRectangle(cornerRadius: 18)
                                .fill(Color.purple.opacity(0.12))
                                .overlay(RoundedRectangle(cornerRadius: 18)
                                    .stroke(lavender.opacity(0.28), lineWidth: 0.5))
                        )
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 52)
            }
        }
    }
}

// MARK: - Placeholder main view (replace with your actual app)

struct MainView: View {
    @EnvironmentObject private var state: AppState
    private let cream = Color(red: 0.91, green: 0.87, blue: 0.78)
    private let lavender = Color(red: 0.78, green: 0.72, blue: 0.91)

    var body: some View {
        ZStack {
            StarFieldView().ignoresSafeArea()
            VStack(spacing: 12) {
                Spacer()
                Text("✓")
                    .font(.system(size: 48))
                    .foregroundColor(lavender)
                Text(L.isZh ? "夜間模式已啟動" : "Night mode active")
                    .font(.system(size: 22, weight: .light, design: .serif))
                    .foregroundColor(cream)
                Text(L.isZh
                     ? "每晚 \(state.reminderFormatted) 自動執行"
                     : "Runs automatically at \(state.reminderFormatted)")
                    .font(.system(size: 14))
                    .foregroundColor(Color(white: 0.35))
                Spacer()
            }
        }
    }
}

// MARK: - Preview

#Preview("Onboarding") {
    ContentView()
}
