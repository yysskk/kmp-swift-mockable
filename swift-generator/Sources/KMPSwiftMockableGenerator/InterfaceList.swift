import Foundation

/// The Kotlin interfaces annotated with `@SwiftMockable`, as listed by the symbol
/// processor in `kmp-swift-mockable/interfaces.json`.
package struct InterfaceList: Codable, Equatable, Sendable {
    /// An annotated interface.
    package struct Entry: Codable, Equatable, Sendable {
        /// The interface's fully qualified Kotlin name, e.g. `com.example.UserRepository`.
        package let kotlinName: String
        /// The name Kotlin/Native exports the interface under, e.g. `UserRepository`.
        package let swiftName: String

        package init(kotlinName: String, swiftName: String) {
            self.kotlinName = kotlinName
            self.swiftName = swiftName
        }
    }

    /// The annotated interfaces.
    package let interfaces: [Entry]

    package init(interfaces: [Entry]) {
        self.interfaces = interfaces
    }

    /// Reads a list written by the symbol processor.
    package init(contentsOf url: URL) throws {
        self = try JSONDecoder().decode(Self.self, from: Data(contentsOf: url))
    }
}
