# How it works

A Swift macro sees only the declaration it is attached to. `@Mockable` cannot be attached to a protocol imported from a Kotlin framework, and could not read its requirements if it were. kmp-swift-mockable therefore writes each Kotlin interface back out as Swift source, a *mirror*, and lets swift-mockable mock that.

## The build steps

1. **Symbol processor.** During Kotlin compilation for each Apple target, the processor collects the interfaces annotated with `@SwiftMockable` and writes their Kotlin names and exported Swift names to `kmp-swift-mockable/interfaces.json` in KSP's resource output. It generates no code.
2. **Link.** Kotlin/Native links the framework. Its `Headers/` and `Modules/module.modulemap` describe the Objective-C interface Kotlin/Native exported.
3. **Synthesize.** The plugin's `GenerateSwiftMocks` task, which finalizes the link task, runs the generator. The generator runs `xcrun swift-synthesize-interface -print-fully-qualified-types` on the framework: the same tool Xcode uses to show the generated interface of an Objective-C module.
4. **Mirror.** The generator parses that Swift text with SwiftParser, picks the protocols named in the interface list, and writes a `@Mockable` protocol for each. The file is written only when its contents change.
5. **Expand.** The Swift test target compiles the file, and swift-mockable's macro generates `<Name>Mock` for each mirror.

## Why read the synthesized interface

Kotlin/Native and the Swift compiler's Clang importer together decide what a Kotlin interface looks like from Swift: `Int` becomes `Int32`, `List<User>` becomes `[Shared.User]`, nullability follows Kotlin's, names are disambiguated, and `suspend` functions become `async throws`. Rather than reimplement those rules, the generator reads their result, so it keeps working as Kotlin and Swift evolve them.

The interface is synthesized with fully qualified types (`Shared.User`, `Swift.Int32`). The generator drops only `Swift.` and keeps every other module qualifier. A mirror named `UserRepository` would otherwise shadow `Shared.UserRepository` wherever a signature mentions it.

## The shape of a mirror

```swift
@Mockable
protocol UserRepository: Shared.UserRepository, Repository {
    func fetchUser(id: Int32) async throws -> Shared.User
    func close()
}
```

**The first parent is the exported protocol.** swift-mockable treats a parent named with a module qualifier other than `Swift` as a conformance: the mock conforms to it through the mirror, without subclassing anything for it. That is what lets Kotlin code accept the mock.

**At most one mirrored parent.** swift-mockable makes a child mock subclass its parent's mock, and a class has one superclass. The mirror lists the first inherited interface that is mirrored too, so `UserRepositoryMock` subclasses `RepositoryMock`. The requirements of every other parent are restated in the mirror instead. That covers a second mirrored parent, a parent without `@SwiftMockable`, and Kotlin runtime protocols such as `KotlinComparable`. Requirements the listed parent's mock already implements are left out, and an inherited protocol the framework does not declare is dropped.

**One requirement per `suspend` function.** Kotlin/Native exports a `suspend` function as an Objective-C method taking a completion handler, which Swift imports twice: as written, and as an `async` function. The two are the same Objective-C method, so a single witness satisfies both, and the mirror keeps only the `async` one. Restating both would compile, but swift-mockable would then disambiguate the two members' names, and `fetchUserHandler` would no longer exist.

**No attributes or comments.** `@available`, `@objc`, and the documentation Kotlin/Native adds to each `suspend` function are dropped; each mirror gets a one-line comment naming the protocol it mirrors.

## Why a mock without `NSObject` works

A Kotlin interface is exported as an Objective-C protocol that does not inherit `NSObject`, and Swift imports it without `@objc` requirements on `NSObjectProtocol`. A Swift class that does not inherit `NSObject` can therefore conform to it, and swift-mockable's mocks are such classes. Kotlin/Native calls into them through the Objective-C runtime like any other object. The project's spike confirmed this with Kotlin 2.4.21, Xcode 27.1, and Swift 6 language mode:

- Kotlin code receiving the mock as its interface calls the mock's `async` method through the completion-handler bridge, for both non-null and nullable results.
- Property reads and writes, overloads (`save(user:)` and `save(users:)`), inherited requirements, and restated requirements all reach the mock.
- No concurrency diagnostics are reported.

The sample's tests ([`sample/swift`](../sample/swift)) check the same behavior on every change.

## Why the tasks are where they are

- **After linking.** The headers exist only once the framework is linked. Finalizing the link task means every build that produces a framework, including Xcode's `embedAndSignAppleFrameworkForXcode`, also refreshes the mocks.
- **Up to date, but not cached.** The task's inputs are the framework's headers and module map rather than its binary, so relinking without an API change leaves it up to date. Each task generates into its own file in the build directory and copies it to the shared file in `outputDir` only when the contents differ, so frameworks never fight over one output and an unchanged build keeps the file's date. It is not stored in the build cache, because a cache hit would skip that copy.
- **KSP applied by the build.** The KSP Gradle plugin must match the Kotlin version, so the plugin leaves applying it to the build rather than pinning a version of its own.
- **macOS only.** `swift-synthesize-interface` comes with Xcode. On other hosts no generation task is registered, so an Android or JVM build is unaffected.
- **A bundled generator.** The plugin's jar carries the generator as a universal macOS binary, about 5 MB compressed, extracted on first use to a directory named after its SHA-256. Using the plugin needs no Swift checkout and no separate download, and a plugin update never runs a stale copy.
