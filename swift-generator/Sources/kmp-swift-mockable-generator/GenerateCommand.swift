import ArgumentParser
import KMPSwiftMockableGenerator

/// Writes Swift protocols that mirror Kotlin Multiplatform interfaces, so that
/// swift-mockable can generate mocks for them.
@main
struct GenerateCommand: ParsableCommand {
    static let configuration = CommandConfiguration(
        commandName: "kmp-swift-mockable-generator",
        abstract: "Generate @Mockable mirror protocols for Kotlin Multiplatform interfaces.",
        version: GeneratorVersion.current
    )

    func run() throws {}
}
