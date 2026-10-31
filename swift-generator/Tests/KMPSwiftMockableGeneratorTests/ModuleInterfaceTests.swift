import Testing

@testable import KMPSwiftMockableGenerator

@Suite("Module Interface Tests")
struct ModuleInterfaceTests {
    private func requirements(of protocolName: String, in source: String) throws -> [String] {
        let declaration = try #require(ModuleInterface(source: source).protocolDeclaration(named: protocolName))
        return declaration.requirements.map(\.declaration.description)
    }

    @Test("Only top-level protocols are read, in source order")
    func readsTopLevelProtocols() {
        let interface = ModuleInterface(source: Fixtures.shared)
        #expect(interface.protocols.map(\.name) == ["Closeable", "Repository", "KotlinComparable", "UserRepository"])
    }

    @Test("A missing protocol is reported as nil")
    func missingProtocol() {
        let interface = ModuleInterface(source: Fixtures.shared)
        #expect(interface.protocolDeclaration(named: "User") == nil)
    }

    @Test("Inherited types are kept as written")
    func inheritedTypes() throws {
        let declaration = try #require(ModuleInterface(source: Fixtures.shared).protocolDeclaration(named: "UserRepository"))
        #expect(declaration.inheritedTypes == ["Shared.Closeable", "Shared.KotlinComparable", "Shared.Repository"])
    }

    @Test("A completion-handler requirement with an async counterpart is dropped")
    func dropsCompletionHandlerVariants() throws {
        #expect(try requirements(of: "UserRepository", in: Fixtures.shared) == [
            "func fetchUser(id: Swift.Int32) async throws -> Shared.User",
            "func findUser(name: Swift.String) async throws -> Shared.User?",
            "func save(users: [Shared.User])",
            "func save(user: Shared.User)",
            "var cachedUser: Shared.User? { get set }",
        ])
    }

    @Test("A completion handler without an async counterpart is kept")
    func keepsUnpairedCompletionHandler() throws {
        let source = """
            public protocol Loader {
                func load(completionHandler: @escaping @Sendable (Swift.Error?) -> Swift.Void)
                func load(id: Swift.Int32) async throws
            }
            """
        #expect(try requirements(of: "Loader", in: source) == [
            "func load(completionHandler: @escaping @Sendable (Swift.Error?) -> Swift.Void)",
            "func load(id: Swift.Int32) async throws",
        ])
    }

    @Test("A disambiguated completion handler label is paired with its async counterpart")
    func pairsDisambiguatedCompletionHandler() throws {
        let source = """
            public protocol Uploader {
                func upload(completionHandler: Swift.String, completionHandler_: @escaping @Sendable ((any Swift.Error)?) -> Swift.Void)
                func upload(completionHandler: Swift.String) async throws
            }
            """
        #expect(try requirements(of: "Uploader", in: source) == [
            "func upload(completionHandler: Swift.String) async throws",
        ])
    }

    @Test("A parameter named like a completion handler but not of function type is not a completion handler")
    func nonFunctionCompletionHandler() throws {
        let source = """
            public protocol Tracker {
                func track(completionHandler: Swift.String)
                func track() async throws
            }
            """
        #expect(try requirements(of: "Tracker", in: source).count == 2)
    }

    @Test("A completion handler is not paired with an async function taking other parameter types")
    func pairingComparesParameterTypes() throws {
        let source = """
            public protocol Store {
                func put(value: Swift.String, completionHandler: @escaping @Sendable ((any Swift.Error)?) -> Swift.Void)
                func put(value: Swift.Int32) async throws
            }
            """
        #expect(try requirements(of: "Store", in: source).count == 2)
    }

    @Test("Protocols and requirements in every branch of an #if block are read")
    func readsConditionalDeclarations() throws {
        let source = """
            #if os(iOS)
            public protocol Camera {
                func capture()
            }
            #else
            public protocol Screen {
                #if DEBUG
                func dump()
                #endif
                func refresh()
            }
            #endif
            """
        let interface = ModuleInterface(source: source)
        #expect(interface.protocols.map(\.name) == ["Camera", "Screen"])
        #expect(try requirements(of: "Screen", in: source) == ["func dump()", "func refresh()"])
    }

    @Test("Attributes and comments are removed from requirements")
    func stripsAttributesAndComments() throws {
        let source = """
            public protocol Legacy {
                /// Deprecated in Kotlin.
                @available(*, deprecated, message: "Use refresh")
                @objc func reload()
                @preconcurrency var title: Swift.String { get }
            }
            """
        #expect(try requirements(of: "Legacy", in: source) == [
            "func reload()",
            "var title: Swift.String { get }",
        ])
    }

    @Test("Every kind of requirement is read with its base name")
    func requirementKinds() throws {
        let source = """
            public protocol Container {
                associatedtype Element
                init(capacity: Swift.Int32)
                subscript(index: Swift.Int32) -> Element { get }
                var count: Swift.Int32 { get }
                func append(_ element: Element)
                typealias Index = Swift.Int32
            }
            """
        let declaration = try #require(ModuleInterface(source: source).protocolDeclaration(named: "Container"))
        #expect(declaration.requirements.map(\.kind) == [.associatedType, .initializer, .subscript, .property, .function])
        #expect(declaration.requirements.map(\.name) == ["Element", "init", "subscript", "count", "append"])
    }

    @Test("A property declared with a pattern other than a name is skipped")
    func skipsTuplePattern() throws {
        let source = """
            public protocol Odd {
                var (a, b): (Swift.Int32, Swift.Int32) { get }
            }
            """
        #expect(try requirements(of: "Odd", in: source).isEmpty)
    }

    @Test("A protocol without an inheritance clause has no inherited types")
    func noInheritance() throws {
        let declaration = try #require(ModuleInterface(source: Fixtures.shared).protocolDeclaration(named: "Closeable"))
        #expect(declaration.inheritedTypes.isEmpty)
    }
}
