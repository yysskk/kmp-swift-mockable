plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
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
    testImplementation(project(":annotations"))
}
