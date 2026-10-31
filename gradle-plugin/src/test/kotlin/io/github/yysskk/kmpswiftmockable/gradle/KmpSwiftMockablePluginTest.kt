package io.github.yysskk.kmpswiftmockable.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KmpSwiftMockablePluginTest {
    private val projectDir: File = Files.createTempDirectory("kmp-swift-mockable").toFile()
    private val pluginVersion = System.getProperty("pluginVersion")
    private val isMacOS = System.getProperty("os.name").startsWith("Mac")

    @AfterTest
    fun deleteProject() {
        projectDir.deleteRecursively()
    }

    /**
     * A Maven repository with empty stand-ins for the annotation and processor artifacts
     * the plugin adds, so that the test builds can resolve them before they are published.
     */
    private fun writeArtifactRepository(): File {
        val repository = projectDir.resolve("artifacts")
        for (artifact in listOf("kmp-swift-mockable-annotations", "kmp-swift-mockable-ksp")) {
            val directory = repository.resolve("io/github/yysskk/$artifact/$pluginVersion").apply { mkdirs() }
            directory.resolve("$artifact-$pluginVersion.pom").writeText(
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>io.github.yysskk</groupId>
                  <artifactId>$artifact</artifactId>
                  <version>$pluginVersion</version>
                </project>
                """.trimIndent(),
            )
            ZipOutputStream(directory.resolve("$artifact-$pluginVersion.jar").outputStream()).close()
        }
        return repository
    }

    private fun writeProject(plugins: String, extra: String = "") {
        val repository = writeArtifactRepository()
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            dependencyResolutionManagement {
                repositories {
                    maven { url = uri("${repository.toURI()}") }
                    mavenCentral()
                }
            }
            rootProject.name = "shared"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
            $plugins
            }
            $extra
            """.trimIndent(),
        )
    }

    private val multiplatformProject = """
        kotlin {
            jvm()
            iosSimulatorArm64 { binaries.framework { baseName = "Shared" } }
            macosArm64 { binaries.framework { baseName = "Shared" } }
        }

        kmpSwiftMockable {
            outputDir = layout.projectDirectory.dir("Generated")
        }
    """.trimIndent()

    private fun runner(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")

    private fun build(vararg arguments: String): BuildResult = runner(*arguments).build()

    @Test
    fun `a project without KSP fails with a fix`() {
        writeProject(
            """
                id("org.jetbrains.kotlin.multiplatform")
                id("io.github.yysskk.kmp-swift-mockable")
            """,
            multiplatformProject,
        )

        val result = runner("help").buildAndFail()
        assertContains(result.output, "The kmp-swift-mockable plugin needs KSP")
        assertContains(result.output, "id(\"com.google.devtools.ksp\")")
    }

    @Test
    fun `a project without Kotlin Multiplatform fails with a fix`() {
        writeProject("""    id("io.github.yysskk.kmp-swift-mockable")""")

        val result = runner("help").buildAndFail()
        assertContains(result.output, "needs the Kotlin Multiplatform plugin")
    }

    @Test
    fun `the annotation and the processor are added at the plugin's version`() {
        writeProject(
            """
                id("org.jetbrains.kotlin.multiplatform")
                id("com.google.devtools.ksp")
                id("io.github.yysskk.kmp-swift-mockable")
            """,
            multiplatformProject + """

            tasks.register("printDependencies") {
                val common = configurations.getByName("commonMainImplementation").dependencies.map { it.toString() }
                val ksp = configurations.getByName("kspIosSimulatorArm64").dependencies.map { it.toString() }
                val jvmKsp = configurations.getByName("kspJvm").dependencies.map { it.toString() }
                doLast {
                    println("common=" + common)
                    println("ksp=" + ksp)
                    println("jvmKsp=" + jvmKsp)
                }
            }
            """.trimIndent(),
        )

        val output = build("printDependencies").output
        assertContains(output, "common=[io.github.yysskk:kmp-swift-mockable-annotations:$pluginVersion]")
        assertContains(output, "ksp=[io.github.yysskk:kmp-swift-mockable-ksp:$pluginVersion]")
        assertContains(output, "jvmKsp=[]")
    }

    @Test
    fun `a generation task follows each framework link on macOS`() {
        writeProject(
            """
                id("org.jetbrains.kotlin.multiplatform")
                id("com.google.devtools.ksp")
                id("io.github.yysskk.kmp-swift-mockable")
            """,
            multiplatformProject,
        )

        val output = build("linkDebugFrameworkIosSimulatorArm64", "--dry-run").output
        if (isMacOS) {
            assertContains(output, ":kspKotlinIosSimulatorArm64 SKIPPED")
            assertContains(output, ":linkDebugFrameworkIosSimulatorArm64 SKIPPED")
            assertContains(output, ":generateSwiftMocksDebugFrameworkIosSimulatorArm64 SKIPPED")
            assertFalse(output.contains(":generateSwiftMocksDebugFrameworkMacosArm64"))
        } else {
            assertFalse(output.contains(":generateSwiftMocks"))
        }
    }

    @Test
    fun `generateSwiftMocks uses the host simulator's debug framework and is configuration cache safe`() {
        writeProject(
            """
                id("org.jetbrains.kotlin.multiplatform")
                id("com.google.devtools.ksp")
                id("io.github.yysskk.kmp-swift-mockable")
            """,
            multiplatformProject,
        )

        val first = build("generateSwiftMocks", "--dry-run", "--configuration-cache").output
        assertContains(first, "Configuration cache entry stored")
        if (isMacOS && System.getProperty("os.arch") == "aarch64") {
            assertContains(first, ":generateSwiftMocksDebugFrameworkIosSimulatorArm64 SKIPPED")
            assertFalse(first.contains(":generateSwiftMocksReleaseFramework"))
            assertFalse(first.contains("MacosArm64 SKIPPED"))
        }

        val second = build("generateSwiftMocks", "--dry-run", "--configuration-cache").output
        assertContains(second, "Configuration cache entry reused")
    }

    @Test
    fun `the generator must be configured before a task runs`() {
        if (!isMacOS) return
        writeProject(
            """
                id("org.jetbrains.kotlin.multiplatform")
                id("com.google.devtools.ksp")
                id("io.github.yysskk.kmp-swift-mockable")
            """,
            multiplatformProject,
        )
        val framework = projectDir.resolve("build/bin/iosSimulatorArm64/debugFramework/Shared.framework")
        framework.resolve("Headers").mkdirs()
        framework.resolve("Modules").mkdirs()

        val result = runner("generateSwiftMocksDebugFrameworkIosSimulatorArm64", "-x", "linkDebugFrameworkIosSimulatorArm64", "-x", "kspKotlinIosSimulatorArm64")
            .buildAndFail()
        assertContains(result.output, "kmp-swift-mockable has no generator to run")
        assertTrue(projectDir.resolve("Generated").listFiles().isNullOrEmpty())
    }
}
