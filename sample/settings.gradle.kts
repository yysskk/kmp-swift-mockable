// A Kotlin Multiplatform project using kmp-swift-mockable as an app would. The plugin,
// the annotation, and the processor come from the enclosing build instead of a
// repository, so the sample always exercises the code next to it.
pluginManagement {
    includeBuild("..")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("..")

rootProject.name = "sample"

include(":shared")
