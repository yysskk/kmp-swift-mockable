import SwiftParser
import SwiftSyntax

/// The protocols of a Swift module interface, as printed by
/// `swift-synthesize-interface` for a Kotlin/Native framework.
///
/// Kotlin interfaces are exported to Objective-C, so the synthesized interface shows
/// each one as a plain `public protocol` whose requirements Swift has already
/// translated: Kotlin types are mapped, names are disambiguated, and every `suspend`
/// function appears twice, once taking a completion handler and once `async`.
package struct ModuleInterface {
    /// The protocols declared at the top level of the interface, in source order.
    package let protocols: [ProtocolDeclaration]

    /// Parses the text of a synthesized module interface.
    package init(source: String) {
        let file = Parser.parse(source: source)
        protocols = file.statements.compactMap { statement in
            statement.item.as(ProtocolDeclSyntax.self).map(ProtocolDeclaration.init)
        }
    }

    /// The protocol with the given name, if the interface declares one.
    package func protocolDeclaration(named name: String) -> ProtocolDeclaration? {
        protocols.first { $0.name == name }
    }
}

/// A protocol read from a module interface.
package struct ProtocolDeclaration {
    /// The protocol's name.
    package let name: String
    /// The inherited types as written, e.g. `Shared.Repository`, in source order.
    package let inheritedTypes: [String]
    /// The requirements a conforming type has to witness, in source order.
    ///
    /// A completion-handler requirement with an `async` counterpart is left out: both
    /// are the same Objective-C method, so witnessing the `async` one satisfies both.
    package let requirements: [Requirement]

    init(_ decl: ProtocolDeclSyntax) {
        name = decl.name.text
        inheritedTypes = decl.inheritanceClause?.inheritedTypes.map(\.type.trimmedDescription) ?? []

        let members = decl.memberBlock.members.compactMap { Requirement($0.decl) }
        let asyncSignatures = Set(members.lazy.filter(\.isAsync).map(\.selectorSignature))
        requirements = members.filter { member in
            guard let signature = member.completionHandlerAsyncSignature else {
                return true
            }
            return !asyncSignatures.contains(signature)
        }
    }
}

/// A protocol requirement with its attributes, comments, and surrounding whitespace
/// removed, ready to be restated in another protocol.
package struct Requirement {
    /// What kind of declaration the requirement is.
    package enum Kind: Equatable {
        case function
        case property
        case `subscript`
        case initializer
        case associatedType
    }

    /// What kind of declaration the requirement is.
    package let kind: Kind
    /// The requirement's base name: `fetchUser` for `func fetchUser(id:)`, `name` for
    /// `var name`, `init` and `subscript` for those declarations.
    package let name: String
    /// The declaration without attributes or comments.
    package let declaration: DeclSyntax

    /// Whether the requirement is an `async` function.
    var isAsync: Bool {
        declaration.as(FunctionDeclSyntax.self)?.signature.effectSpecifiers?.asyncSpecifier != nil
    }

    /// The base name and argument labels, e.g. `fetchUser(id:)`.
    var selectorSignature: String {
        guard let function = declaration.as(FunctionDeclSyntax.self) else {
            return name
        }
        return Self.signature(name: name, parameters: Array(function.signature.parameterClause.parameters))
    }

    /// For a function whose last parameter is a Kotlin/Native completion handler, the
    /// signature of the `async` function Swift imports for the same method: the same
    /// base name and labels without the completion handler. `nil` for any other
    /// requirement.
    var completionHandlerAsyncSignature: String? {
        guard let function = declaration.as(FunctionDeclSyntax.self),
              function.signature.effectSpecifiers?.asyncSpecifier == nil,
              let last = function.signature.parameterClause.parameters.last,
              Self.isCompletionHandlerLabel(last.firstName.text),
              Self.isFunctionType(last.type)
        else {
            return nil
        }
        let parameters = function.signature.parameterClause.parameters.dropLast()
        return Self.signature(name: name, parameters: Array(parameters))
    }

    init?(_ decl: DeclSyntax) {
        if let function = decl.as(FunctionDeclSyntax.self) {
            kind = .function
            name = function.name.text
            declaration = DeclSyntax(function.with(\.attributes, []).trimmed)
        } else if let variable = decl.as(VariableDeclSyntax.self) {
            guard let binding = variable.bindings.first?.pattern.as(IdentifierPatternSyntax.self) else {
                return nil
            }
            kind = .property
            name = binding.identifier.text
            declaration = DeclSyntax(variable.with(\.attributes, []).trimmed)
        } else if let subscriptDecl = decl.as(SubscriptDeclSyntax.self) {
            kind = .subscript
            name = "subscript"
            declaration = DeclSyntax(subscriptDecl.with(\.attributes, []).trimmed)
        } else if let initializer = decl.as(InitializerDeclSyntax.self) {
            kind = .initializer
            name = "init"
            declaration = DeclSyntax(initializer.with(\.attributes, []).trimmed)
        } else if let associatedType = decl.as(AssociatedTypeDeclSyntax.self) {
            kind = .associatedType
            name = associatedType.name.text
            declaration = DeclSyntax(associatedType.with(\.attributes, []).trimmed)
        } else {
            return nil
        }
    }

    /// Kotlin/Native names the parameter `completionHandler`, and appends underscores
    /// when a Kotlin parameter already has that name.
    private static func isCompletionHandlerLabel(_ label: String) -> Bool {
        label.hasPrefix("completionHandler") && label.dropFirst("completionHandler".count).allSatisfy { $0 == "_" }
    }

    private static func isFunctionType(_ type: TypeSyntax) -> Bool {
        var type = type
        while let attributed = type.as(AttributedTypeSyntax.self) {
            type = attributed.baseType
        }
        return type.is(FunctionTypeSyntax.self)
    }

    private static func signature(name: String, parameters: [FunctionParameterSyntax]) -> String {
        let labels = parameters.map { "\($0.firstName.text):" }.joined()
        return "\(name)(\(labels))"
    }
}
