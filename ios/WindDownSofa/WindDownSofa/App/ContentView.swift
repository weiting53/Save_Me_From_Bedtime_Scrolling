import SwiftUI

struct ContentView: View {
    @State private var selectedTab = 0
    @State private var showWindDown = false

    var body: some View {
        ZStack {
            TabView(selection: $selectedTab) {
                HomeView(showWindDown: $showWindDown)
                    .tabItem {
                        Label("首頁", systemImage: "moon.stars.fill")
                    }
                    .tag(0)

                HistoryView()
                    .tabItem {
                        Label("記錄", systemImage: "chart.bar.fill")
                    }
                    .tag(1)

                SettingsView()
                    .tabItem {
                        Label("設定", systemImage: "gearshape.fill")
                    }
                    .tag(2)
            }
            .tint(Color.accentPurple)
        }
        .fullScreenCover(isPresented: $showWindDown) {
            WindDownView(isPresented: $showWindDown)
        }
        .onReceive(NotificationCenter.default.publisher(for: .openWindDown)) { _ in
            showWindDown = true
        }
    }
}
