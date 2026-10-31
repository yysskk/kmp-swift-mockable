package io.github.yysskk.kmpswiftmockable

/**
 * Generates a Swift mock of the annotated interface for iOS tests.
 *
 * The kmp-swift-mockable Gradle plugin writes a Swift protocol that mirrors the
 * interface as Kotlin/Native exports it, marked with swift-mockable's `@Mockable`.
 * swift-mockable then generates `<Name>Mock`, which conforms to the exported
 * protocol and can be passed wherever Kotlin expects the interface:
 *
 * ```kotlin
 * @SwiftMockable
 * interface UserRepository {
 *     suspend fun fetchUser(id: Int): User
 * }
 * ```
 *
 * ```swift
 * let repository = UserRepositoryMock()
 * repository.fetchUserHandler = { id in User(id: id, name: "Alice") }
 * let viewModel = UserViewModel(repository: repository)
 * ```
 *
 * The interface must be declared in the module that builds the Apple framework.
 * Applying the annotation to anything other than an interface is a compile error.
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class SwiftMockable
