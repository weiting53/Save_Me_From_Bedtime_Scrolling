import SwiftUI

// MARK: - Fox State

enum FoxState {
    case drowsy    // 首頁：眼睛半閉 + zzz
    case sleeping  // WindDown：完全閉眼 + 月亮
    case lively    // Onboarding：正常眼睛 + 笑容
    case curious   // 空狀態：歪頭 + 問號
}

// MARK: - FoxMascotView (PRD §7.6)

struct FoxMascotView: View {
    let state: FoxState

    @State private var eyeCloseness: CGFloat = 0
    @State private var floatOffset: CGFloat = 0
    @State private var zzzOpacity: Double = 0

    var body: some View {
        ZStack {
            foxBody
            foxTail
            foxFace

            if state == .drowsy || state == .sleeping {
                zzzLabel
                    .offset(x: 48, y: -60)
                    .opacity(zzzOpacity)
            }

            if state == .sleeping {
                moonDecoration
                    .offset(x: 55, y: -80)
            }

            if state == .curious {
                Image(systemName: "questionmark")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(Color.accentStar)
                    .offset(x: 48, y: -72)
            }
        }
        .offset(y: floatOffset)
        .rotationEffect(state == .curious ? .degrees(-8) : .degrees(0))
        .onAppear { startAnimations() }
    }

    // MARK: - Fox Body

    private var foxBody: some View {
        Ellipse()
            .fill(Color.foxOrange)
            .frame(width: 100, height: 80)
            .offset(y: 20)
    }

    private var foxTail: some View {
        ZStack {
            Ellipse()
                .fill(Color.foxOrange)
                .frame(width: 60, height: 28)
                .rotationEffect(.degrees(-30))
                .offset(x: 60, y: 32)
            Ellipse()
                .fill(Color.foxCream)
                .frame(width: 32, height: 18)
                .rotationEffect(.degrees(-30))
                .offset(x: 68, y: 30)
        }
    }

    private var foxFace: some View {
        ZStack {
            // Head
            Circle()
                .fill(Color.foxOrange)
                .frame(width: 90, height: 90)
                .offset(y: -18)

            // Ears
            foxEar(xOffset: -24)
            foxEar(xOffset: 24)

            // Face details
            foxFaceDetails
        }
    }

    private func foxEar(xOffset: CGFloat) -> some View {
        ZStack {
            Triangle()
                .fill(Color.foxOrange)
                .frame(width: 26, height: 28)
                .offset(x: xOffset, y: -52)
            Triangle()
                .fill(Color(hex: "#C04000"))
                .frame(width: 14, height: 16)
                .offset(x: xOffset, y: -52)
        }
    }

    private var foxFaceDetails: some View {
        ZStack {
            // Snout
            Ellipse()
                .fill(Color.foxCream)
                .frame(width: 44, height: 28)
                .offset(y: -4)

            // Nose
            Circle()
                .fill(Color(hex: "#C04000"))
                .frame(width: 10, height: 8)
                .offset(y: -12)

            // Eyes
            foxEyes

            // Mouth
            if state == .lively {
                Arc(startAngle: .degrees(0), endAngle: .degrees(180), clockwise: false)
                    .stroke(Color(hex: "#C04000"), lineWidth: 2)
                    .frame(width: 16, height: 8)
                    .offset(y: -2)
            }
        }
        .offset(y: -18)
    }

    private var foxEyes: some View {
        HStack(spacing: 22) {
            FoxEye(closeness: eyeCloseness)
            FoxEye(closeness: eyeCloseness)
        }
        .offset(y: -20)
    }

    private var zzzLabel: some View {
        Text("z z z")
            .font(.system(size: 14, weight: .medium))
            .foregroundColor(Color.accentStar)
    }

    private var moonDecoration: some View {
        Image(systemName: "moon.fill")
            .font(.system(size: 20))
            .foregroundColor(Color.accentStar)
    }

    // MARK: - Animations

    private func startAnimations() {
        switch state {
        case .sleeping:
            eyeCloseness = 1.0
            withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true)) {
                floatOffset = -8
            }
            withAnimation(.easeInOut(duration: 1.5).repeatForever(autoreverses: true).delay(0.5)) {
                zzzOpacity = 1.0
            }
        case .drowsy:
            withAnimation(.easeInOut(duration: 0.8)) {
                eyeCloseness = 0.55
            }
            withAnimation(.easeInOut(duration: 2.5).repeatForever(autoreverses: true)) {
                floatOffset = -5
            }
            withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true).delay(1.0)) {
                zzzOpacity = 0.8
            }
        case .lively:
            eyeCloseness = 0
            withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true)) {
                floatOffset = -6
            }
        case .curious:
            eyeCloseness = 0
            withAnimation(.easeInOut(duration: 1.8).repeatForever(autoreverses: true)) {
                floatOffset = -4
            }
        }
    }
}

// MARK: - Fox Eye

private struct FoxEye: View {
    let closeness: CGFloat

    private var openHeight: CGFloat { 12 }
    private var lidOffset: CGFloat { openHeight * closeness }

    var body: some View {
        ZStack {
            // Sclera
            Ellipse()
                .fill(Color(hex: "#2A1A0A"))
                .frame(width: 14, height: openHeight)

            // Eyelid
            Rectangle()
                .fill(Color.foxOrange)
                .frame(width: 16, height: openHeight)
                .offset(y: -openHeight / 2 + lidOffset)
        }
        .frame(width: 14, height: openHeight)
        .clipped()
    }
}

// MARK: - Custom Shapes

private struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.closeSubpath()
        return path
    }
}

private struct Arc: Shape {
    var startAngle: Angle
    var endAngle: Angle
    var clockwise: Bool

    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.addArc(
            center: CGPoint(x: rect.midX, y: rect.midY),
            radius: rect.width / 2,
            startAngle: startAngle,
            endAngle: endAngle,
            clockwise: clockwise
        )
        return path
    }
}
