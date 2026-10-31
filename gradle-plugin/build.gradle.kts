import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
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
    plugins {
        create("kmpSwiftMockable") {
            id = "io.github.yysskk.kmp-swift-mockable"
            implementationClass = "io.github.yysskk.kmpswiftmockable.gradle.KmpSwiftMockablePlugin"
            displayName = "kmp-swift-mockable"
            description = "Generates Swift mocks of Kotlin Multiplatform interfaces with swift-mockable."
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
