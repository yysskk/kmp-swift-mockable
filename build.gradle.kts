plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.maven.publish) apply false
}

// Coverage of the JVM modules, merged into build/reports/kover/report.xml.
dependencies {
    kover(project(":kmp-swift-mockable-ksp"))
    kover(project(":kmp-swift-mockable-gradle-plugin"))
}

allprojects {
    group = property("GROUP") as String
    version = property("VERSION_NAME") as String
}
