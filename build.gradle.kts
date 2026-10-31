plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kover)
}

// Coverage of the JVM modules, merged into build/reports/kover/report.xml.
dependencies {
    kover(project(":ksp"))
}

allprojects {
    group = property("GROUP") as String
    version = property("VERSION_NAME") as String
}
