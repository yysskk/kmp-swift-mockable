# Getting started

This guide adds kmp-swift-mockable to a Kotlin Multiplatform project with a `shared` module that builds a framework named `Shared`, and an iOS app with a test target. It covers the two common ways the framework reaches Swift: an Xcode build phase, and a Swift package.

## Kotlin side

Apply KSP and the plugin in `shared/build.gradle.kts`, next to the Kotlin Multiplatform plugin:

```kotlin
plugins {
    kotlin("multiplatform") version "2.4.21"
    id("com.google.devtools.ksp") version "2.3.13"
    id("io.github.yysskk.kmp-swift-mockable") version "1.0.0"
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
        }
    }
}

kmpSwiftMockable {
    outputDir = file("../iosApp/iosAppTests/Generated")
}
```

Then annotate an interface:

```kotlin
// shared/src/commonMain/kotlin/com/example/UserRepository.kt
package com.example

import io.github.yysskk.kmpswiftmockable.SwiftMockable

data class User(val id: Int, val name: String)

@SwiftMockable
interface UserRepository {
    suspend fun fetchUser(id: Int): User
    var cachedUser: User?
}

class UserViewModel(private val repository: UserRepository) {
    var name: String? = null
        private set

    suspend fun load(id: Int) {
        name = repository.fetchUser(id).name
    }
}
```

Check the setup by generating once by hand:

```sh
./gradlew :shared:generateSwiftMocks
```

This links the debug framework for the simulator matching your Mac and writes `iosApp/iosAppTests/Generated/SharedMocks.swift`.

## Xcode app

Projects created with the Kotlin Multiplatform wizard build the framework from a "Run Script" build phase of the app target, which runs `embedAndSignAppleFrameworkForXcode`. That task links the framework for the active SDK and architecture, and the plugin generates the mocks right after, so every Xcode build keeps them current.

1. Make sure the build phase runs with `ENABLE_USER_SCRIPT_SANDBOXING = NO`. Kotlin Multiplatform requires it for this phase, and the plugin needs it too, to write into the test target's folder.
2. Add the `Mockable` library from `https://github.com/yysskk/swift-mockable` (4.0.0 or later) to the test target.
3. Add `Generated/SharedMocks.swift` to the test target. With a synchronized folder, Xcode picks it up once it exists; otherwise drag it into the target once.
4. Ignore the generated folder in version control:

   ```gitignore
   iosApp/iosAppTests/Generated/
   ```

5. Write a test:

   ```swift
   import Shared
   import Testing

   struct UserViewModelTests {
       @Test func loadsTheUserName() async throws {
           let repository = UserRepositoryMock()
           repository.fetchUserHandler = { id in User(id: id, name: "Alice") }
           let viewModel = UserViewModel(repository: repository)

           try await viewModel.load(id: 7)

           #expect(viewModel.name == "Alice")
           #expect(repository.fetchUserCallArguments == [7])
       }
   }
   ```

Because the test target depends on the app target, building the tests runs the build phase first, which links the framework and regenerates `SharedMocks.swift` before the test target compiles.

## Swift package

When the framework is packaged as an XCFramework and consumed by a Swift package, generate the mocks into the package's test target:

```kotlin
// shared/build.gradle.kts
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

kotlin {
    val xcframework = XCFramework("Shared")
    listOf(iosSimulatorArm64(), macosArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            xcframework.add(this)
        }
    }
}

kmpSwiftMockable {
    outputDir = file("../swift/Tests/SharedTests/Generated")
}
```

```swift
// swift/Package.swift
let package = Package(
    name: "App",
    platforms: [.iOS(.v16), .macOS(.v14)],
    dependencies: [
        .package(url: "https://github.com/yysskk/swift-mockable.git", from: "4.0.0"),
    ],
    targets: [
        .binaryTarget(name: "Shared", path: "../shared/build/XCFrameworks/debug/Shared.xcframework"),
        .testTarget(
            name: "SharedTests",
            dependencies: ["Shared", .product(name: "Mockable", package: "swift-mockable")]
        ),
    ]
)
```

Build the XCFramework, which also generates the mocks, then run the tests:

```sh
./gradlew :shared:assembleSharedDebugXCFramework
swift test --package-path swift
```

[`sample/`](../sample) in this repository is a complete project set up this way, and its tests show the mocks driving Kotlin code.
