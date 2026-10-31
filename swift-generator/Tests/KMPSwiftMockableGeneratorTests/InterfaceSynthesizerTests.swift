import Foundation
import Testing

@testable import KMPSwiftMockableGenerator

@Suite("Interface Synthesizer Tests")
struct InterfaceSynthesizerTests {
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
    /// Writes a header-only framework shaped like a Kotlin/Native export: an interface
    /// becomes a protocol without `NSObject`, and a suspend function takes a completion
    /// handler.
    private func makeSampleFramework() throws -> URL {
        let framework = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathComponent("Sample.framework")
        let headers = framework.appendingPathComponent("Headers")
        let modules = framework.appendingPathComponent("Modules")
        try FileManager.default.createDirectory(at: headers, withIntermediateDirectories: true)
        try FileManager.default.createDirectory(at: modules, withIntermediateDirectories: true)
        try """
            #import <Foundation/Foundation.h>

            NS_ASSUME_NONNULL_BEGIN

            __attribute__((swift_name("Greeter")))
            @protocol SampleGreeter
            @required
            - (NSString *)greetName:(NSString *)name __attribute__((swift_name("greet(name:)")));
            - (void)loadId:(int32_t)id completionHandler:(void (^)(NSString * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("load(id:completionHandler:)")));
            @end

            NS_ASSUME_NONNULL_END
            """.write(to: headers.appendingPathComponent("Sample.h"), atomically: true, encoding: .utf8)
        try """
            framework module Sample {
                umbrella header "Sample.h"

                export *
                module * { export * }
            }
            """.write(to: modules.appendingPathComponent("module.modulemap"), atomically: true, encoding: .utf8)
        return framework
    }

    private var hostTarget: String {
        #if arch(arm64)
        "arm64-apple-macos13.0"
        #else
        "x86_64-apple-macos13.0"
        #endif
    }

    @Test("A framework's protocols are synthesized and mirrored")
    func synthesizesFramework() throws {
        let framework = try makeSampleFramework()
        defer { try? FileManager.default.removeItem(at: framework.deletingLastPathComponent()) }
        let synthesizer = InterfaceSynthesizer(frameworkURL: framework, target: hostTarget, sdk: "macosx")
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
            frameworkURL: FileManager.default.temporaryDirectory.appendingPathComponent("Missing.framework"),
            target: hostTarget,
            sdk: "macosx"
        )
        let error = try #require(throws: InterfaceSynthesizer.Failure.self) { try synthesizer.synthesize() }
        #expect(error.description.contains("Missing"))
        #expect(!error.description.contains("\n"))
    }
    #endif
}
