import Foundation

/// Prints the Swift interface of a framework with `swift-synthesize-interface`, the
/// tool Xcode uses to show the generated interface of an Objective-C module.
package struct InterfaceSynthesizer: Sendable {
    /// An error running the synthesizer.
    package struct Failure: Error, CustomStringConvertible, Equatable {
        package let description: String
    }

    /// The `.framework` bundle whose module is synthesized.
    package let frameworkURL: URL
    /// The target triple the framework was built for, e.g. `arm64-apple-ios16.0-simulator`.
    package let target: String
    /// The SDK, as a name (`iphonesimulator`) or a path.
    package let sdk: String

    package init(frameworkURL: URL, target: String, sdk: String) {
        self.frameworkURL = frameworkURL
        self.target = target
        self.sdk = sdk
    }

    /// The module name, taken from the framework bundle's name.
    package var moduleName: String {
        frameworkURL.deletingPathExtension().lastPathComponent
    }

    /// Runs the synthesizer and returns the interface's text.
    package func synthesize() throws -> String {
        let sdkPath = sdk.contains("/") ? sdk : try Self.run(["--sdk", sdk, "--show-sdk-path"])
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return try Self.run(["swift-synthesize-interface"] + arguments(sdkPath: sdkPath))
    }

    /// The synthesizer's arguments. Types are printed fully qualified, so that a
    /// mirror never resolves a type to a declaration of the same name in the test target.
    func arguments(sdkPath: String) -> [String] {
        [
            "-module-name", moduleName,
            "-F", frameworkURL.deletingLastPathComponent().path,
            "-target", target,
            "-sdk", sdkPath,
            "-print-fully-qualified-types",
        ]
    }

    /// Runs `xcrun` with the arguments and returns its standard output.
    private static func run(_ arguments: [String]) throws -> String {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/xcrun")
        process.arguments = arguments
        let output = Pipe()
        let error = Pipe()
        process.standardOutput = output
        process.standardError = error
        try process.run()
        // Drain standard error while standard output is read, so that neither pipe
        // can fill up and block the process.
        let errorData = ErrorCollector()
        let group = DispatchGroup()
        DispatchQueue.global().async(group: group) {
            errorData.data = error.fileHandleForReading.readDataToEndOfFile()
        }
        let outputData = output.fileHandleForReading.readDataToEndOfFile()
        group.wait()
        process.waitUntilExit()
        guard process.terminationStatus == 0 else {
            // A missing module is followed by every module the SDK can see, so only
            // the first line is worth reporting.
            let message = String(decoding: errorData.data, as: UTF8.self)
                .split(separator: "\n").first.map(String.init) ?? ""
            throw Failure(description: "xcrun \(arguments.joined(separator: " ")) failed: \(message)")
        }
        return String(decoding: outputData, as: UTF8.self)
    }
}

/// Standard error read on another thread; `DispatchGroup.wait()` orders the write
/// before the read.
private final class ErrorCollector: @unchecked Sendable {
    var data = Data()
}
