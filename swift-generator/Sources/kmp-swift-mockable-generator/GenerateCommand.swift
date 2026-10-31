import ArgumentParser
import Foundation
import KMPSwiftMockableGenerator

/// Writes Swift protocols that mirror Kotlin Multiplatform interfaces, so that
/// swift-mockable can generate mocks for them.
@main
struct GenerateCommand: ParsableCommand {
    static let configuration = CommandConfiguration(
        commandName: "kmp-swift-mockable-generator",
        abstract: "Generate @Mockable mirror protocols for Kotlin Multiplatform interfaces.",
        discussion: """
            Reads the Swift interface of a Kotlin/Native framework, either by running \
            swift-synthesize-interface on it (--framework, --target, --sdk) or from a file \
            (--interface-file, --module), and writes a mirror protocol for every interface \
            listed in the --interfaces files.
            """,
        version: GeneratorVersion.current
    )

    @Option(help: "The Kotlin/Native .framework bundle to read.", completion: .directory)
    var framework: String?

    @Option(help: "The target triple the framework was built for, e.g. arm64-apple-ios16.0-simulator.")
    var target: String?

    @Option(help: "The SDK the framework was built against, as a name (iphonesimulator) or a path.")
    var sdk: String?

    @Option(help: "A file holding an already synthesized interface, instead of --framework.", completion: .file())
    var interfaceFile: String?

    @Option(help: "The framework's module name. Defaults to the name of the --framework bundle.")
    var module: String?

    @Option(
        help: "A list of interfaces to mirror, written by the symbol processor. Repeat for several lists.",
        completion: .file(extensions: ["json"])
    )
    var interfaces: [String] = []

    @Option(help: "The compilation condition of the generated mocks. Defaults to swift-mockable's DEBUG.")
    var condition: String?

    @Option(help: "The access level of the mirror protocols: internal, package, or public.")
    var accessLevel: AccessLevel = .internal

    @Option(help: "The Swift file to write.", completion: .file(extensions: ["swift"]))
    var output: String

    func validate() throws {
        switch (framework, interfaceFile) {
        case (.some, .some):
            throw ValidationError("Pass either --framework or --interface-file, not both.")
        case (.none, .none):
            throw ValidationError("Pass --framework, or --interface-file with --module.")
        case (.some, .none):
            guard target != nil, sdk != nil else {
                throw ValidationError("--framework needs --target and --sdk.")
            }
        case (.none, .some):
            guard module != nil else {
                throw ValidationError("--interface-file needs --module.")
            }
            guard target == nil, sdk == nil else {
                throw ValidationError("--target and --sdk apply to --framework only.")
            }
        }
    }

    func run() throws {
        let (moduleName, interfaceSource) = try readInterface()
        let names = try interfaces.flatMap { path in
            try InterfaceList(contentsOf: URL(fileURLWithPath: path)).interfaces.map(\.swiftName)
        }
        let generator = MirrorGenerator(
            moduleName: moduleName,
            interface: ModuleInterface(source: interfaceSource),
            options: MirrorOptions(condition: condition, accessLevel: accessLevel)
        )
        let file = generator.generate(protocolNames: names)
        for name in file.missingProtocolNames {
            FileHandle.standardError.write(Data("warning: \(moduleName) exports no protocol named '\(name)'\n".utf8))
        }
        try GeneratedFile.write(file.source, to: URL(fileURLWithPath: output))
    }

    private func readInterface() throws -> (moduleName: String, source: String) {
        if let interfaceFile, let module {
            return (module, try String(contentsOfFile: interfaceFile, encoding: .utf8))
        }
        guard let framework, let target, let sdk else {
            throw ValidationError("Pass --framework, or --interface-file with --module.")
        }
        let synthesizer = InterfaceSynthesizer(frameworkURL: URL(fileURLWithPath: framework), target: target, sdk: sdk)
        return (module ?? synthesizer.moduleName, try synthesizer.synthesize())
    }
}

extension AccessLevel: ExpressibleByArgument {}
