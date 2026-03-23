import SwiftUI

// MARK: - Color System (PRD §7.2)

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3:
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }

    // Backgrounds
    static let backgroundPrimary = Color(hex: "#13132B")
    static let backgroundSecondary = Color(hex: "#1E1E42")
    static let backgroundTertiary = Color(hex: "#2A2A5A")

    // Text
    static let textPrimary = Color(hex: "#E8D5C0")
    static let textSecondary = Color(hex: "#8888BB")
    static let textHint = Color(hex: "#5555AA")

    // Accent
    static let accentPurple = Color(hex: "#3A3A7A")
    static let accentStar = Color(hex: "#C8C8F0")

    // Fox
    static let foxOrange = Color(hex: "#C97C3A")
    static let foxCream = Color(hex: "#F0E8D8")

    // Card border
    static let cardBorder = Color(hex: "#3D3D6B")
    static let navBorder = Color(hex: "#5555AA")
}

// MARK: - Typography (PRD §7.3)

enum AppFont {
    static func appName() -> Font {
        .custom("Georgia", size: 22)
    }

    static func subtitle() -> Font {
        .system(size: 12, weight: .regular, design: .rounded)
    }

    static func timeDisplay() -> Font {
        .custom("Georgia", size: 28)
    }

    static func timeDisplayLarge() -> Font {
        .custom("Georgia", size: 30)
    }

    static func cardLabel() -> Font {
        .system(size: 10, weight: .regular)
    }

    static func buttonText() -> Font {
        .system(size: 15, weight: .regular, design: .rounded)
    }

    static func bodyText() -> Font {
        .system(size: 13, weight: .regular)
    }
}

// MARK: - Animation Constants (PRD §7.5)

enum AppAnimation {
    static let screenTransition: Animation = .easeInOut(duration: 0.4)
    static let cardAppear: Animation = .easeOut(duration: 0.5)
    static let foxEye: Animation = .easeInOut(duration: 0.8)
    static let streak: Animation = .spring(response: 0.4, dampingFraction: 0.6)
    static let carousel: Animation = .interactiveSpring(response: 0.3)
    static let buttonPress: Animation = .easeInOut(duration: 0.1)
}

// MARK: - Layout Constants

enum AppLayout {
    static let cardCornerRadius: CGFloat = 16
    static let navCornerRadius: CGFloat = 22
    static let buttonCornerRadius: CGFloat = 26
    static let buttonHeight: CGFloat = 52
    static let cardPaddingV: CGFloat = 16
    static let cardPaddingH: CGFloat = 20
    static let cardBorderWidth: CGFloat = 0.5
    static let navBorderWidth: CGFloat = 0.8
    static let dotSizeUnselected: CGFloat = 6
    static let dotSizeSelected: CGFloat = 10
}

// MARK: - View Modifiers

struct CardStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(.vertical, AppLayout.cardPaddingV)
            .padding(.horizontal, AppLayout.cardPaddingH)
            .background(Color.backgroundSecondary)
            .cornerRadius(AppLayout.cardCornerRadius)
            .overlay(
                RoundedRectangle(cornerRadius: AppLayout.cardCornerRadius)
                    .stroke(Color.cardBorder, lineWidth: AppLayout.cardBorderWidth)
            )
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(AppFont.buttonText())
            .foregroundColor(Color.textPrimary)
            .frame(maxWidth: .infinity)
            .frame(height: AppLayout.buttonHeight)
            .background(Color.accentPurple)
            .cornerRadius(AppLayout.buttonCornerRadius)
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .opacity(configuration.isPressed ? 0.85 : 1.0)
            .animation(AppAnimation.buttonPress, value: configuration.isPressed)
    }
}

struct CardLabelStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .font(AppFont.cardLabel())
            .foregroundColor(Color.textHint)
            .tracking(1.5)
            .textCase(.uppercase)
    }
}

extension View {
    func cardStyle() -> some View {
        modifier(CardStyle())
    }

    func cardLabel() -> some View {
        modifier(CardLabelStyle())
    }
}
