package io.github.yysskk.kmpswiftmockable.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Applies the plugin in-process, to check how it configures a project. */
class PluginConfigurationTest {
    private val isMacOS = System.getProperty("os.name").startsWith("Mac")

    private fun multiplatformProject(configure: Project.() -> Unit = {}): Project {
        val project = ProjectBuilder.builder().withName("shared").build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply("com.google.devtools.ksp")
        project.pluginManager.apply(KmpSwiftMockablePlugin::class.java)
        project.extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
            jvm()
            iosSimulatorArm64 { binaries.framework { baseName = "Shared" } }
        }
        project.configure()
        (project as ProjectInternal).evaluate()
        return project
    }

    private fun Project.generateTask(): GenerateSwiftMocks =
        tasks.getByName("generateSwiftMocksDebugFrameworkIosSimulatorArm64") as GenerateSwiftMocks

    @Test
    fun `the extension has conventions`() {
        val project = multiplatformProject()
        val extension = project.extensions.getByType(KmpSwiftMockableExtension::class.java)
        assertEquals(project.layout.buildDirectory.dir("generated/kmp-swift-mockable").get(), extension.outputDir.get())
        assertEquals(SwiftAccessLevel.INTERNAL, extension.accessLevel.get())
        assertNull(extension.condition.orNull)
    }

    @Test
    fun `each framework gets a generation task configured from the extension`() {
        if (!isMacOS) return
        val project = multiplatformProject {
            extensions.configure(KmpSwiftMockableExtension::class.java) {
                it.outputDir.set(layout.projectDirectory.dir("Generated"))
                it.condition.set("TESTING")
                it.accessLevel.set(SwiftAccessLevel.PUBLIC)
                it.generatorExecutable.set(layout.projectDirectory.file("bin/generator"))
            }
        }

        val task = project.generateTask()
        assertEquals("arm64-apple-ios14.0-simulator", task.targetTriple.get())
        assertEquals("iphonesimulator", task.sdk.get())
        assertEquals(project.file("Generated/SharedMocks.swift"), task.destinationFile.get().asFile)
        assertEquals(
            project.layout.buildDirectory.file("kmp-swift-mockable/iosSimulatorArm64/debugFramework/SharedMocks.swift").get().asFile,
            task.generatedFile.get().asFile,
        )
        assertTrue(task.framework.get().asFile.path.endsWith("bin/iosSimulatorArm64/debugFramework/Shared.framework"))
        assertNotNull(project.tasks.findByName("generateSwiftMocksReleaseFrameworkIosSimulatorArm64"))

        val listFile = project.layout.buildDirectory
            .file("generated/ksp/iosSimulatorArm64/iosSimulatorArm64Main/resources/kmp-swift-mockable/interfaces.json")
            .get().asFile
        listFile.parentFile.mkdirs()
        listFile.writeText("{\"interfaces\": []}")

        assertEquals(
            listOf(
                project.file("bin/generator").absolutePath,
                "--framework", task.framework.get().asFile.absolutePath,
                "--target", "arm64-apple-ios14.0-simulator",
                "--sdk", "iphonesimulator",
                "--interfaces", listFile.absolutePath,
                "--condition", "TESTING",
                "--access-level", "public",
                "--output", task.generatedFile.get().asFile.absolutePath,
            ),
            task.generatorCommandLine(),
        )
    }

    @Test
    fun `a missing interface list and condition are left out of the command line`() {
        if (!isMacOS) return
        val project = multiplatformProject {
            extensions.configure(KmpSwiftMockableExtension::class.java) {
                it.generatorExecutable.set(layout.projectDirectory.file("bin/generator"))
            }
        }

        val commandLine = project.generateTask().generatorCommandLine()
        assertTrue("--interfaces" !in commandLine)
        assertTrue("--condition" !in commandLine)
        assertEquals("internal", commandLine[commandLine.indexOf("--access-level") + 1])
    }

    @Test
    fun `running without a generator is an error`() {
        if (!isMacOS) return
        val task = multiplatformProject().generateTask()
        val error = assertFailsWith<GradleException> { task.generatorCommandLine() }
        assertTrue(error.message!!.contains("kmpSwiftMockable.generatorExecutable"))
    }

    @Test
    fun `the generated file is copied to the destination only when it differs`() {
        if (!isMacOS) return
        val task = multiplatformProject {
            extensions.configure(KmpSwiftMockableExtension::class.java) {
                it.outputDir.set(layout.projectDirectory.dir("Generated"))
            }
        }.generateTask()
        val generated = task.generatedFile.get().asFile.apply { parentFile.mkdirs(); writeText("one") }
        val destination = task.destinationFile.get().asFile

        task.copyToDestination()
        assertEquals("one", destination.readText())

        val modified = destination.lastModified() - 10_000
        destination.setLastModified(modified)
        task.copyToDestination()
        assertEquals(modified, destination.lastModified())

        generated.writeText("two")
        task.copyToDestination()
        assertEquals("two", destination.readText())
    }

    @Test
    fun `non-Apple targets get no processor`() {
        val project = multiplatformProject()
        assertTrue(project.configurations.getByName("kspJvm").dependencies.isEmpty())
        assertEquals(1, project.configurations.getByName("kspIosSimulatorArm64").dependencies.size)
    }
}
