import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    id("io.github.yysskk.kmp-swift-mockable")
}

kotlin {
    val xcframework = XCFramework("Shared")
    listOf(iosSimulatorArm64(), macosArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcframework.add(this)
        }
    }
}

kmpSwiftMockable {
    outputDir = layout.projectDirectory.dir("../swift/Tests/SharedTests/Generated")
}
