package io.github.yysskk.kmpswiftmockable.gradle

/** The access level of the generated Swift protocols, and so of their mocks. */
public enum class SwiftAccessLevel(internal val swiftKeyword: String) {
    /** Visible within the Swift module that compiles the generated file, typically the test target. */
    INTERNAL("internal"),

    /** Visible to other modules of the same Swift package. */
    PACKAGE("package"),

    /** Visible to any module, for a test-support module that other test targets import. */
    PUBLIC("public"),
}
