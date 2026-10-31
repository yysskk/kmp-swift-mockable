pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kmp-swift-mockable"

// Each project is named after the artifact it publishes, so that a build including
// this one replaces those artifacts with the projects.
include(":kmp-swift-mockable-annotations")
include(":kmp-swift-mockable-ksp")
include(":kmp-swift-mockable-gradle-plugin")
project(":kmp-swift-mockable-annotations").projectDir = file("annotations")
project(":kmp-swift-mockable-ksp").projectDir = file("ksp")
project(":kmp-swift-mockable-gradle-plugin").projectDir = file("gradle-plugin")
