package io.github.yysskk.kmpswiftmockable.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * Configures the Swift file the plugin writes for each Apple framework:
 *
 * ```kotlin
 * kmpSwiftMockable {
 *     outputDir = file("../iosApp/AppTests/Generated")
 *     condition = "DEBUG"
 *     accessLevel = SwiftAccessLevel.PUBLIC
 * }
 * ```
 */
public abstract class KmpSwiftMockableExtension {
    /**
     * The directory the generated `<FrameworkName>Mocks.swift` is written to, usually
     * inside the Xcode test target. Defaults to `build/generated/kmp-swift-mockable`.
     */
    public abstract val outputDir: DirectoryProperty

    /**
     * The compilation condition of the generated mocks, passed to swift-mockable's
     * `@Mockable(condition:)`. When unset, swift-mockable's default, `DEBUG`, applies.
     */
    public abstract val condition: Property<String>

    /** The access level of the generated protocols and their mocks. Defaults to [SwiftAccessLevel.INTERNAL]. */
    public abstract val accessLevel: Property<SwiftAccessLevel>

    /** The `kmp-swift-mockable-generator` executable to run. */
    public abstract val generatorExecutable: RegularFileProperty
}
