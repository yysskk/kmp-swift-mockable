import SwiftSyntax

/// The access level of the generated mirror protocols, and so of their mocks.
package enum AccessLevel: String, CaseIterable, Sendable {
    case `internal`
    case `package`
    case `public`
}

/// Options for the generated file.
package struct MirrorOptions: Sendable {
    /// The compilation condition passed to `@Mockable(condition:)`, or `nil` to keep
    /// swift-mockable's default, `DEBUG`.
    package var condition: String?
    /// The access level of the mirror protocols.
    package var accessLevel: AccessLevel

    package init(condition: String? = nil, accessLevel: AccessLevel = .internal) {
        self.condition = condition
        self.accessLevel = accessLevel
    }
}

/// A generated Swift file, and the requested protocols it could not mirror.
package struct MirrorFile: Equatable, Sendable {
    /// The file's source.
    package let source: String
    /// Requested protocol names that the module interface does not declare.
    package let missingProtocolNames: [String]
}

/// Writes a `@Mockable` protocol for each requested protocol of a module, restating
/// its requirements so that swift-mockable can generate a mock for it.
///
/// A mirror refines the protocol it restates, named with its module
/// (`protocol UserRepository: Shared.UserRepository`), so a mock of the mirror is
/// usable wherever Kotlin expects the interface. Inherited protocols are handled as
/// follows:
///
/// - The first inherited protocol that is mirrored as well is listed by its mirror's
///   name, so the mock subclasses that mirror's mock.
/// - The requirements of every other inherited protocol, mirrored or not, are
///   restated in the mirror itself. swift-mockable subclasses only one parent mock,
///   and a protocol the module declares without mirroring has no mock at all.
/// - An inherited protocol the interface does not declare, such as one from another
///   module, is dropped.
package struct MirrorGenerator {
    /// The module the protocols are read from, e.g. `Shared`.
    package let moduleName: String
    /// The module's synthesized interface.
    package let interface: ModuleInterface
    /// Options for the generated file.
    package let options: MirrorOptions

    package init(moduleName: String, interface: ModuleInterface, options: MirrorOptions = MirrorOptions()) {
        self.moduleName = moduleName
        self.interface = interface
        self.options = options
    }

    /// Generates mirrors of the named protocols, in the order the interface declares them.
    package func generate(protocolNames: some Sequence<String>) -> MirrorFile {
        let requested = Set(protocolNames)
        let mirrored = interface.protocols.filter { requested.contains($0.name) }
        let mirroredNames = Set(mirrored.map(\.name))
        let missing = requested.subtracting(mirroredNames).sorted()

        let declarations = mirrored.map { mirror(of: $0, mirroredNames: mirroredNames) }
        return MirrorFile(source: fileSource(declarations: declarations), missingProtocolNames: missing)
    }

    private func mirror(of declaration: ProtocolDeclaration, mirroredNames: Set<String>) -> String {
        let parent = inheritedProtocols(of: declaration).first { mirroredNames.contains($0.name) }
        let inherited = parent.map { allRequirements(of: $0) } ?? []
        var restated = Set(inherited.map(\.key))
        let requirements = allRequirements(of: declaration).filter { restated.insert($0.key).inserted }

        let inheritance = ["\(moduleName).\(declaration.name)"] + (parent.map { [$0.name] } ?? [])
        let accessModifier = options.accessLevel == .internal ? "" : "\(options.accessLevel.rawValue) "
        let body = requirements.map { "    \($0.text)\n" }.joined()
        return """
            /// Mirrors `\(moduleName).\(declaration.name)` for swift-mockable.
            \(mockableAttribute)
            \(accessModifier)protocol \(declaration.name): \(inheritance.joined(separator: ", ")) {
            \(body)}
            """
    }

    /// The protocol's requirements followed by those of every protocol it inherits,
    /// depth first, each restated once. Each protocol is visited once, so a protocol
    /// inherited along several paths, or an interface that is not valid Swift and
    /// inherits itself, cannot make the walk repeat or recurse forever.
    private func allRequirements(of declaration: ProtocolDeclaration) -> [RestatedRequirement] {
        var visited = Set<String>()
        var seen = Set<String>()
        var result: [RestatedRequirement] = []
        func visit(_ declaration: ProtocolDeclaration) {
            guard visited.insert(declaration.name).inserted else {
                return
            }
            for requirement in declaration.requirements {
                let restated = RestatedRequirement(requirement)
                if seen.insert(restated.key).inserted {
                    result.append(restated)
                }
            }
            inheritedProtocols(of: declaration).forEach(visit)
        }
        visit(declaration)
        return result
    }

    /// The inherited protocols the interface declares, in the order they are written.
    private func inheritedProtocols(of declaration: ProtocolDeclaration) -> [ProtocolDeclaration] {
        let prefix = "\(moduleName)."
        return declaration.inheritedTypes.compactMap { type in
            guard type.hasPrefix(prefix) else {
                return nil
            }
            return interface.protocolDeclaration(named: String(type.dropFirst(prefix.count)))
        }
    }

    private var mockableAttribute: String {
        guard let condition = options.condition else {
            return "@Mockable"
        }
        return "@Mockable(condition: \(Self.stringLiteral(condition)))"
    }

    /// The text as a Swift string literal, so the attribute always parses; swift-mockable
    /// then reports a condition that is not a valid compilation condition.
    private static func stringLiteral(_ text: String) -> String {
        var escaped = ""
        for character in text {
            switch character {
            case "\\": escaped += "\\\\"
            case "\"": escaped += "\\\""
            case "\n": escaped += "\\n"
            case "\r": escaped += "\\r"
            case "\t": escaped += "\\t"
            default: escaped.append(character)
            }
        }
        return "\"\(escaped)\""
    }

    private func fileSource(declarations: [String]) -> String {
        let header = """
            // Generated by kmp-swift-mockable \(GeneratorVersion.current). Do not edit.

            import Foundation
            import Mockable
            import \(moduleName)

            """
        return header + declarations.map { "\n\($0)\n" }.joined()
    }
}

/// A requirement as written in a mirror: with the `Swift.` module qualifier removed
/// from its types, which keeps it readable. Other qualifiers stay, so a type always
/// names the module's declaration even where a mirror of the same name is in scope.
private struct RestatedRequirement {
    /// The requirement's source.
    let text: String
    /// The source with whitespace collapsed, identifying a requirement restated by
    /// more than one inherited protocol.
    let key: String

    init(_ requirement: Requirement) {
        text = SwiftQualifierRemover().rewrite(requirement.declaration).description
        key = text.split(whereSeparator: \.isWhitespace).joined(separator: " ")
    }
}

/// Removes the `Swift.` qualifier from member types: `Swift.Int32` becomes `Int32`.
private final class SwiftQualifierRemover: SyntaxRewriter {
    override func visit(_ node: MemberTypeSyntax) -> TypeSyntax {
        guard let base = node.baseType.as(IdentifierTypeSyntax.self),
              base.name.text == "Swift",
              base.genericArgumentClause == nil
        else {
            return super.visit(node)
        }
        let unqualified = IdentifierTypeSyntax(
            name: node.name.with(\.leadingTrivia, node.leadingTrivia),
            genericArgumentClause: node.genericArgumentClause.map { visit($0) }
        )
        return TypeSyntax(unqualified)
    }
}
