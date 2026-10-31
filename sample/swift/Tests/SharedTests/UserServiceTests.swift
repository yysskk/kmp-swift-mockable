import Shared
import Testing

@Suite("User Service Tests")
struct UserServiceTests {
    @Test("Kotlin awaits a suspend function implemented by the mock")
    func suspendFunction() async throws {
        let repository = UserRepositoryMock()
        repository.fetchUserHandler = { id in User(id: id, name: "Alice") }
        let analytics = AnalyticsMock()
        let service = UserService(repository: repository, analytics: analytics)

        #expect(try await service.displayName(id: 7) == "Alice")
        #expect(repository.fetchUserCallArguments == [7])
        #expect(repository.cachedUser?.name == "Alice")
        #expect(analytics.trackCallArguments.map(\.event) == ["user_loaded"])
        #expect(analytics.trackCallArguments.first?.properties == ["id": "7"])
    }

    @Test("Kotlin reads a property set on the mock instead of calling it")
    func cachedProperty() async throws {
        let repository = UserRepositoryMock()
        repository.cachedUser = User(id: 7, name: "Cached")
        let service = UserService(repository: repository, analytics: AnalyticsMock())

        #expect(try await service.displayName(id: 7) == "Cached")
        #expect(repository.fetchUserCallCount == 0)
    }

    @Test("A suspend function returning nil reaches Kotlin as null")
    func nullableSuspendFunction() async throws {
        let repository = UserRepositoryMock()
        repository.findUserHandler = { _ in nil }
        let service = UserService(repository: repository, analytics: AnalyticsMock())

        // A suspend function boxes its Kotlin `Boolean` result.
        #expect(try await service.exists(name: "Bob").boolValue == false)
        #expect(repository.findUserCallArguments == ["Bob"])
    }

    @Test("Overloads are mocked separately")
    func overloads() {
        let repository = UserRepositoryMock()
        let service = UserService(repository: repository, analytics: AnalyticsMock())

        service.importUsers(users: [User(id: 1, name: "One")])
        service.importUsers(users: [User(id: 1, name: "One"), User(id: 2, name: "Two")])

        #expect(repository.saveUserCallCount == 1)
        #expect(repository.saveUsersCallCount == 1)
    }

    @Test("Inherited and restated requirements are mocked")
    func inheritedRequirements() {
        let repository = UserRepositoryMock()
        repository.name = "users"
        let service = UserService(repository: repository, analytics: AnalyticsMock())

        service.shutdown()

        #expect(service.repositoryName() == "users")
        #expect(repository.clearCallCount == 1)
        #expect(repository.closeCallCount == 1)
        // The mirrored parent's mock is the superclass.
        let parent: RepositoryMock = repository
        #expect(parent.clearCallCount == 1)
    }
}
