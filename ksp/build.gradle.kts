plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
    alias(libs.plugins.maven.publish)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.ksp.api)

    testImplementation(kotlin("test"))
    testImplementation(libs.ksp.api)
    testImplementation(libs.kotlin.compile.testing.ksp)
    testImplementation(project(":kmp-swift-mockable-annotations"))
}

mavenPublishing {
    publishToMavenCentral()
    // Release builds sign with the key CI passes as signingInMemoryKey; local
    // publishing to Maven Local works without one.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    pom {
        name = "kmp-swift-mockable symbol processor"
        description = "A KSP processor that lists the interfaces annotated with @SwiftMockable for the kmp-swift-mockable Gradle plugin."
    }
}
