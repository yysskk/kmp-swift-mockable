import Foundation
import Testing

@testable import KMPSwiftMockableGenerator

@Suite("Interface Synthesizer Tests")
struct InterfaceSynthesizerTests {
    private let sampleFramework = Bundle.module.resourceURL!
        .appendingPathComponent("Resources/Sample.framework")

    @Test("The module name is the framework's name")
    func moduleName() {
        let synthesizer = InterfaceSynthesizer(
            frameworkURL: URL(fileURLWithPath: "/build/bin/Shared.framework"),
            target: "arm64-apple-ios16.0-simulator",
            sdk: "iphonesimulator"
        )
        #expect(synthesizer.moduleName == "Shared")
    }

    @Test("The framework's directory is the search path, and types are fully qualified")
    func arguments() {
        let synthesizer = InterfaceSynthesizer(
            frameworkURL: URL(fileURLWithPath: "/build/bin/Shared.framework"),
            target: "arm64-apple-ios16.0-simulator",
            sdk: "iphonesimulator"
        )
        #expect(synthesizer.arguments(sdkPath: "/SDKs/iPhoneSimulator.sdk") == [
            "-module-name", "Shared",
            "-F", "/build/bin",
            "-target", "arm64-apple-ios16.0-simulator",
            "-sdk", "/SDKs/iPhoneSimulator.sdk",
            "-print-fully-qualified-types",
        ])
    }

    #if os(macOS)
    private var hostTarget: String {
        #if arch(arm64)
        "arm64-apple-macos13.0"
        #else
        "x86_64-apple-macos13.0"
        #endif
    }

    @Test("A framework's protocols are synthesized and mirrored")
    func synthesizesFramework() throws {
        let synthesizer = InterfaceSynthesizer(frameworkURL: sampleFramework, target: hostTarget, sdk: "macosx")
        let interface = ModuleInterface(source: try synthesizer.synthesize())
        let file = MirrorGenerator(moduleName: synthesizer.moduleName, interface: interface)
            .generate(protocolNames: ["Greeter"])

        #expect(file.source.hasSuffix("""
            /// Mirrors `Sample.Greeter` for swift-mockable.
            @Mockable
            protocol Greeter: Sample.Greeter {
                func greet(name: String) -> String
                func load(id: Int32) async throws -> String?
            }

            """))
    }

    @Test("A module the synthesizer cannot load is reported on one line")
    func missingModule() throws {
        let synthesizer = InterfaceSynthesizer(
            frameworkURL: sampleFramework.deletingLastPathComponent().appendingPathComponent("Missing.framework"),
            target: hostTarget,
            sdk: "macosx"
        )
        let error = try #require(throws: InterfaceSynthesizer.Failure.self) { try synthesizer.synthesize() }
        #expect(error.description.contains("Missing"))
        #expect(!error.description.contains("\n"))
    }
    #endif
}
