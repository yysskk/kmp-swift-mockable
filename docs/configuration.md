# Configuration

## The `kmpSwiftMockable` extension

```kotlin
import io.github.yysskk.kmpswiftmockable.gradle.SwiftAccessLevel

kmpSwiftMockable {
    outputDir = file("../iosApp/iosAppTests/Generated")
    condition = "DEBUG || TESTING"
    accessLevel = SwiftAccessLevel.PUBLIC
    generatorExecutable = file("../kmp-swift-mockable/swift-generator/.build/debug/kmp-swift-mockable-generator")
}
```

### `outputDir`

The directory the generated file is written to. Defaults to `build/generated/kmp-swift-mockable` in the module.

The file is named after the framework's base name: `SharedMocks.swift` for `baseName = "Shared"`. Every framework of the module, debug or release, for whichever target, generates into its own file under `build/kmp-swift-mockable/` and copies it to this one, which therefore reflects the framework linked last. The mirrors depend almost only on the Kotlin interfaces, so the frameworks normally agree. A module building frameworks with different base names writes one file per base name.

### `condition`

The compilation condition of the generated mocks, passed to swift-mockable as `@Mockable(condition: "...")`. When unset, the attribute has no argument, and swift-mockable's default applies: the mocks exist only in `DEBUG` builds.

Any condition swift-mockable accepts works: a flag (`"TESTING"`), or an expression (`"DEBUG || UITESTS"`, `"os(iOS) && DEBUG"`). Define every flag it names in the Swift target that compiles the file.

### `accessLevel`

The access level of the mirror protocols, which swift-mockable gives to the mocks too. One of:

| Value | Use |
| --- | --- |
| `SwiftAccessLevel.INTERNAL` (default) | The file is compiled into the test target that uses the mocks. |
| `SwiftAccessLevel.PACKAGE` | The file is in a Swift package target, used by other targets of the same package. |
| `SwiftAccessLevel.PUBLIC` | The file is in a test-support module that other test targets import. |

### `generatorExecutable`

The `kmp-swift-mockable-generator` executable to run. By default the plugin runs the universal macOS binary bundled in its jar, extracted on first use to `~/.gradle/caches/kmp-swift-mockable/generator/`. Set this to run another build of it, for example while working on the generator:

```sh
swift build --package-path swift-generator
```

```kotlin
kmpSwiftMockable {
    generatorExecutable = file("<checkout>/swift-generator/.build/debug/kmp-swift-mockable-generator")
}
```

## Tasks

| Task | Description |
| --- | --- |
| `generateSwiftMocks<Binary><Target>` | Generates from one framework, e.g. `generateSwiftMocksDebugFrameworkIosSimulatorArm64`. Runs after that framework's link task, which it finalizes. |
| `generateSwiftMocks` | Generates from the debug framework of the host's simulator (`iosSimulatorArm64` on Apple silicon, `iosX64` on Intel), or else from the first Apple framework. Links it first if needed. |

The tasks are registered on macOS only. They are up-to-date checked against the framework's `Headers` and `Modules` directories, the interface list, and the options, so relinking without an API change runs nothing. A task is also rerun when the shared file in `outputDir` no longer matches its own output, for example after another framework replaced it. The tasks are not stored in the build cache, because a cache hit would skip copying to `outputDir`.

## Dependencies the plugin adds

| Configuration | Dependency |
| --- | --- |
| `commonMainImplementation` | `io.github.yysskk:kmp-swift-mockable-annotations` |
| `ksp<AppleTarget>`, for every Apple target | `io.github.yysskk:kmp-swift-mockable-ksp` |

Both have the plugin's version. The annotation has source retention, so nothing of it ends up in the framework.

## The generator command line

The plugin runs the generator like this; it can also be run directly:

```text
kmp-swift-mockable-generator
    --framework <path to .framework> --target <triple> --sdk <name or path>
    | --interface-file <synthesized interface> --module <module name>
    [--interfaces <interfaces.json>]...
    [--condition <condition>]
    [--access-level internal|package|public]
    --output <file.swift>
```

`--interfaces` takes the list the symbol processor writes to `build/generated/ksp/<target>/<target>Main/resources/kmp-swift-mockable/interfaces.json`:

```json
{"interfaces": [
    {"kotlinName": "com.example.UserRepository", "swiftName": "UserRepository"}
]}
```

A listed name the framework does not export is reported as a warning. The output is written only when its contents change.
