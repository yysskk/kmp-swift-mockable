import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.io.ByteArrayOutputStream
import javax.inject.Inject

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
    alias(libs.plugins.plugin.publish)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
    compilerOptions {
        // Gradle runs plugins with the Kotlin version it embeds, so the plugin is
        // compiled for the oldest one among the Gradle versions it supports.
        apiVersion = KotlinVersion.KOTLIN_2_0
        languageVersion = KotlinVersion.KOTLIN_2_0
    }
}

gradlePlugin {
    website = "https://github.com/yysskk/kmp-swift-mockable"
    vcsUrl = "https://github.com/yysskk/kmp-swift-mockable"
    plugins {
        create("kmpSwiftMockable") {
            id = "io.github.yysskk.kmp-swift-mockable"
            implementationClass = "io.github.yysskk.kmpswiftmockable.gradle.KmpSwiftMockablePlugin"
            displayName = "kmp-swift-mockable"
            description = "Generates Swift mocks of Kotlin Multiplatform interfaces with swift-mockable."
            tags = listOf("kotlin-multiplatform", "swift", "ios", "testing", "mock")
        }
    }
}

dependencies {
    // The Kotlin Gradle plugin is the one the build applies; the plugin only reads
    // its model, so it is not bundled.
    compileOnly(libs.kotlin.gradle.plugin)

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
    testImplementation(libs.kotlin.gradle.plugin)
    testImplementation(libs.ksp.gradle.plugin)
}

/**
 * Builds `kmp-swift-mockable-generator` as a stripped universal macOS binary, which the
 * jar bundles so that the plugin runs without a Swift checkout.
 */
@DisableCachingByDefault(because = "Building with SwiftPM is not worth caching between machines.")
abstract class BuildGenerator @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifest: RegularFileProperty

    @get:OutputFile
    abstract val executable: RegularFileProperty

    @TaskAction
    fun build() {
        val arguments = listOf(
            "swift", "build", "-c", "release", "--arch", "arm64", "--arch", "x86_64",
            "--package-path", manifest.get().asFile.parent,
        )
        execOperations.exec { commandLine(arguments) }
        // The products' directory depends on the SwiftPM build system, so it is asked for.
        val binPath = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(arguments + "--show-bin-path")
            standardOutput = binPath
        }
        val built = File(binPath.toString().trim(), "kmp-swift-mockable-generator")
        val output = executable.get().asFile
        built.copyTo(output, overwrite = true)
        execOperations.exec { commandLine("xcrun", "strip", "-x", output.absolutePath) }
    }
}

// Only macOS can build the generator. A jar built elsewhere has none, and the plugin
// then asks for kmpSwiftMockable.generatorExecutable.
val buildGenerator = tasks.register<BuildGenerator>("buildGenerator") {
    description = "Builds the generator bundled in the plugin's jar."
    val packageDirectory = rootProject.layout.projectDirectory.dir("swift-generator")
    sources = packageDirectory.dir("Sources")
    manifest = packageDirectory.file("Package.swift")
    executable = layout.buildDirectory.file("generator/kmp-swift-mockable-generator")
    onlyIf("the generator builds on macOS only") { System.getProperty("os.name").startsWith("Mac") }
}

tasks.jar {
    from(buildGenerator.flatMap { it.executable }) {
        into("kmp-swift-mockable/generator")
    }
}

// The plugin adds the annotation and processor artifacts at its own version.
tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("kmp-swift-mockable.properties") {
        expand("version" to version)
    }
}

// TestKit loads the plugin under test in its own class loader, which cannot see a
// Kotlin plugin resolved by the test build, so the Kotlin and KSP plugins are loaded
// alongside it and the test builds apply them without a version.
val testPluginClasspath: Configuration = configurations.create("testPluginClasspath")

dependencies {
    testPluginClasspath(libs.kotlin.gradle.plugin)
    testPluginClasspath(libs.ksp.gradle.plugin)
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(testPluginClasspath)
}

tasks.test {
    systemProperty("pluginVersion", project.version.toString())
}
