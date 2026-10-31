/// Module interfaces shaped like the output of `swift-synthesize-interface
/// -print-fully-qualified-types` for a Kotlin/Native framework named `Shared`.
enum Fixtures {
    /// Kotlin source:
    ///
    /// ```kotlin
    /// interface Closeable { fun close() }
    /// interface Repository { val name: String; fun clear() }
    /// interface UserRepository : Repository, Closeable, Comparable<UserRepository> {
    ///     suspend fun fetchUser(id: Int): User
    ///     suspend fun findUser(name: String): User?
    ///     var cachedUser: User?
    ///     fun save(user: User)
    ///     fun save(users: List<User>)
    /// }
    /// ```
    static let shared = """
        import Foundation

        @available(*, unavailable)
        public class KotlinBase : NSObject {
            public init()
        }

        public protocol Closeable {

            func close()
        }
        public protocol Repository {

            func clear()

            var name: Swift.String { get }
        }

        public class User : Shared.KotlinBase {

            public init(id: Swift.Int32, name: Swift.String)

            open var id: Swift.Int32 { get }
        }

        public protocol KotlinComparable {

            func compareTo(other: Any?) -> Swift.Int32
        }

        public protocol UserRepository : Shared.Closeable, Shared.KotlinComparable, Shared.Repository {

            /**
             * @note This method converts instances of CancellationException to errors.
             * Other uncaught Kotlin exceptions are fatal.
            */
            func fetchUser(id: Swift.Int32, completionHandler: @escaping @Sendable (Shared.User?, (any Swift.Error)?) -> Swift.Void)

            /**
             * @note This method converts instances of CancellationException to errors.
             * Other uncaught Kotlin exceptions are fatal.
            */
            func fetchUser(id: Swift.Int32) async throws -> Shared.User

            /**
             * @note This method converts instances of CancellationException to errors.
             * Other uncaught Kotlin exceptions are fatal.
            */
            func findUser(name: Swift.String, completionHandler: @escaping @Sendable (Shared.User?, (any Swift.Error)?) -> Swift.Void)

            /**
             * @note This method converts instances of CancellationException to errors.
             * Other uncaught Kotlin exceptions are fatal.
            */
            func findUser(name: Swift.String) async throws -> Shared.User?

            func save(users: [Shared.User])

            func save(user: Shared.User)

            var cachedUser: Shared.User? { get set }
        }
        """
}
