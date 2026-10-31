import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

// The annotation is applied in common code, so it is published for every target a
// Kotlin Multiplatform project can have, not only the Apple ones it acts on.
kotlin {
    explicitApi()

    jvm()
    js {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    iosArm64()
    iosX64()
    iosSimulatorArm64()
    macosArm64()
    macosX64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosX64()
    watchosSimulatorArm64()
    linuxArm64()
    linuxX64()
    mingwX64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()
}

mavenPublishing {
    publishToMavenCentral()
    // Release builds sign with the key CI passes as signingInMemoryKey; local
    // publishing to Maven Local works without one.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    pom {
        name = "kmp-swift-mockable annotations"
        description = "The @SwiftMockable annotation, which marks Kotlin interfaces to generate Swift mocks for."
    }
}
