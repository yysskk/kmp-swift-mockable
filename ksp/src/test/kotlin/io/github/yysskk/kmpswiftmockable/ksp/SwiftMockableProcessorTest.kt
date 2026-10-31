package io.github.yysskk.kmpswiftmockable.ksp

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class SwiftMockableProcessorTest {
    /**
     * `kotlin.native.ObjCName` exists only in native and common code. The JVM
     * compilation used here gets a stand-in with the same name and parameters.
     */
    private val objCName = SourceFile.kotlin(
        "ObjCName.kt",
        """
        package kotlin.native

        annotation class ObjCName(val name: String = "", val swiftName: String = "", val exact: Boolean = false)
        """,
    )

    private class Outcome(val result: JvmCompilationResult, val interfaceList: String?)

    private fun process(source: String): Outcome {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Sources.kt", source), objCName)
            inheritClassPath = true
            kotlincArguments = listOf("-Xallow-kotlin-package")
            configureKsp { symbolProcessorProviders += SwiftMockableProcessorProvider() }
        }
        val result = compilation.compile()
        val list = compilation.kspSourcesDir.walkTopDown()
            .firstOrNull { it.isFile && it.invariantSeparatorsPath.endsWith("kmp-swift-mockable/interfaces.json") }
        return Outcome(result, list?.readText())
    }

    @Test
    fun `annotated interfaces are listed with their exported names`() {
        val outcome = process(
            """
            package com.example

            import io.github.yysskk.kmpswiftmockable.SwiftMockable
            import kotlin.native.ObjCName

            @SwiftMockable
            interface UserRepository

            interface Outer {
                @SwiftMockable
                interface Listener
            }

            @SwiftMockable
            @ObjCName(swiftName = "SwiftStore")
            interface Store

            @SwiftMockable
            @ObjCName("NamedCache")
            interface Cache

            interface Plain
            """,
        )

        assertEquals(KotlinCompilation.ExitCode.OK, outcome.result.exitCode, outcome.result.messages)
        assertEquals(
            """
            {"interfaces": [
                {"kotlinName": "com.example.Cache", "swiftName": "NamedCache"},
                {"kotlinName": "com.example.Outer.Listener", "swiftName": "OuterListener"},
                {"kotlinName": "com.example.Store", "swiftName": "SwiftStore"},
                {"kotlinName": "com.example.UserRepository", "swiftName": "UserRepository"}
            ]}

            """.trimIndent(),
            outcome.interfaceList,
        )
    }

    @Test
    fun `a nested interface takes its outer declaration's exported name`() {
        val outcome = process(
            """
            package com.example

            import io.github.yysskk.kmpswiftmockable.SwiftMockable
            import kotlin.native.ObjCName

            @ObjCName(swiftName = "Feed")
            class FeedScreen {
                @SwiftMockable
                interface Presenter
            }
            """,
        )

        assertTrue(outcome.interfaceList.orEmpty().contains(""""swiftName": "FeedPresenter""""), outcome.interfaceList)
    }

    @Test
    fun `an empty list is written when nothing is annotated`() {
        val outcome = process("package com.example\n\ninterface Plain\n")

        assertEquals(KotlinCompilation.ExitCode.OK, outcome.result.exitCode, outcome.result.messages)
        assertEquals("{\"interfaces\": []}\n", outcome.interfaceList)
    }

    @Test
    fun `annotating a class is an error`() {
        val outcome = process(
            """
            package com.example

            import io.github.yysskk.kmpswiftmockable.SwiftMockable

            @SwiftMockable
            class UserRepository
            """,
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, outcome.result.exitCode)
        assertTrue(outcome.result.messages.contains("@SwiftMockable can only be applied to interfaces."))
        assertFalse(outcome.interfaceList.orEmpty().contains("UserRepository"))
    }
}
