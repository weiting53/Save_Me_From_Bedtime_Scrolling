import SwiftUI

// MARK: - MethodCarouselView (PRD §3.1)

struct MethodCarouselView: View {
    @Binding var selectedMethodId: String
    let methods: [AnyWindDownMethod]

    @State private var currentIndex: Int = 0
    @GestureState private var dragOffset: CGFloat = 0

    var body: some View {
        ZStack {
            // Frosted glass background
            RoundedRectangle(cornerRadius: AppLayout.navCornerRadius)
                .fill(.ultraThinMaterial)
                .overlay(
                    RoundedRectangle(cornerRadius: AppLayout.navCornerRadius)
                        .fill(Color.backgroundTertiary.opacity(0.55))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: AppLayout.navCornerRadius)
                        .stroke(Color.navBorder, lineWidth: AppLayout.navBorderWidth)
                )

            HStack(spacing: 0) {
                // Left arrow
                Button {
                    navigateCarousel(direction: -1)
                } label: {
                    Image(systemName: "chevron.left")
                        .foregroundColor(Color.textSecondary)
                        .padding(.leading, 14)
                }

                Spacer()

                // Method display
                VStack(spacing: 6) {
                    Image(systemName: methods[currentIndex].icon)
                        .font(.system(size: 24))
                        .foregroundColor(Color.accentStar)

                    Text(methods[currentIndex].name)
                        .font(AppFont.buttonText())
                        .foregroundColor(Color.textPrimary)

                    Text(methods[currentIndex].description)
                        .font(AppFont.bodyText())
                        .foregroundColor(Color.textSecondary)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .padding(.horizontal, 8)
                }
                .animation(AppAnimation.carousel, value: currentIndex)

                Spacer()

                // Right arrow / Random button
                VStack(spacing: 4) {
                    Button {
                        navigateCarousel(direction: 1)
                    } label: {
                        Image(systemName: "chevron.right")
                            .foregroundColor(Color.textSecondary)
                    }

                    Button {
                        randomizeMethod()
                    } label: {
                        Text("⇄")
                            .font(.system(size: 16))
                            .foregroundColor(Color.textSecondary)
                    }
                }
                .padding(.trailing, 14)
            }
            .padding(.vertical, 16)

            // Page dots at bottom
            VStack {
                Spacer()
                PageIndicatorView(currentPage: currentIndex, totalPages: methods.count)
                    .padding(.bottom, 8)
            }
        }
        .frame(height: 130)
        .gesture(
            DragGesture()
                .onEnded { value in
                    if value.translation.width < -40 {
                        navigateCarousel(direction: 1)
                    } else if value.translation.width > 40 {
                        navigateCarousel(direction: -1)
                    }
                }
        )
        .onAppear {
            if let idx = methods.firstIndex(where: { $0.id == selectedMethodId }) {
                currentIndex = idx
            }
        }
    }

    private func navigateCarousel(direction: Int) {
        withAnimation(AppAnimation.carousel) {
            currentIndex = (currentIndex + direction + methods.count) % methods.count
            selectedMethodId = methods[currentIndex].id
        }
    }

    private func randomizeMethod() {
        let randomIndex = (0..<methods.count).filter { $0 != currentIndex }.randomElement() ?? 0
        withAnimation(AppAnimation.carousel) {
            currentIndex = randomIndex
            selectedMethodId = methods[currentIndex].id
        }
    }
}
