import SwiftUI

// MARK: - WindDownMethod Protocol (PRD §3.5)

protocol WindDownMethod: Identifiable {
    var id: String { get }
    var name: String { get }
    var icon: String { get }
    var description: String { get }
    func view() -> AnyView
}
