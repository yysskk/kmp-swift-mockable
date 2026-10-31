package io.github.yysskk.kmpswiftmockable.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.util.Properties

/**
 * Generates swift-mockable mirrors of the Kotlin interfaces annotated with
 * `@SwiftMockable`, each time an Apple framework is linked.
 *
 * Applied next to the Kotlin Multiplatform and KSP plugins, it:
 * - adds the annotation to `commonMain`, and the symbol processor to every Apple target
 *   that builds a framework;
 * - registers a [GenerateSwiftMocks] task for every Apple framework, run after the
 *   framework links;
 * - registers `generateSwiftMocks`, which generates from the debug framework of the
 *   host's simulator, to run by hand.
 *
 * The Swift interface synthesizer comes with Xcode, so on other hosts no generation
 * task is registered.
 */
public class KmpSwiftMockablePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("kmpSwiftMockable", KmpSwiftMockableExtension::class.java)
        extension.outputDir.convention(project.layout.buildDirectory.dir("generated/kmp-swift-mockable"))
        extension.accessLevel.convention(SwiftAccessLevel.INTERNAL)

        project.pluginManager.withPlugin(KOTLIN_MULTIPLATFORM_PLUGIN) {
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            kotlin.sourceSets.getByName("commonMain").dependencies {
                implementation("$GROUP:kmp-swift-mockable-annotations:$version")
            }
            configureAppleTargets(project, kotlin, extension)
        }

        project.afterEvaluate {
            if (!project.pluginManager.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN)) {
                throw GradleException(
                    "The kmp-swift-mockable plugin needs the Kotlin Multiplatform plugin. " +
                        "Apply id(\"org.jetbrains.kotlin.multiplatform\") in ${project.path}.",
                )
            }
            if (!project.pluginManager.hasPlugin(KSP_PLUGIN)) {
                throw GradleException(
                    "The kmp-swift-mockable plugin needs KSP to find @SwiftMockable interfaces. " +
                        "Apply id(\"$KSP_PLUGIN\") in ${project.path}, next to the kmp-swift-mockable plugin.",
                )
            }
        }
    }

    private fun configureAppleTargets(project: Project, kotlin: KotlinMultiplatformExtension, extension: KmpSwiftMockableExtension) {
        val generateFromHost = project.tasks.register("generateSwiftMocks") { task ->
            task.group = TASK_GROUP
            task.description = "Generates Swift mocks from the debug framework of the host's simulator."
        }
        val isMacOS = System.getProperty("os.name").startsWith("Mac")
        val hostCandidates = mutableListOf<Pair<Framework, TaskProvider<GenerateSwiftMocks>>>()
        generateFromHost.configure { task ->
            task.dependsOn(project.provider { listOfNotNull(hostTask(hostCandidates)) })
        }

        kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
            val appleTarget = AppleTarget.forKonanTarget(target.konanTarget.name) ?: return@configureEach
            val kspConfiguration = "ksp" + target.name.replaceFirstChar(Char::uppercaseChar)
            project.pluginManager.withPlugin(KSP_PLUGIN) {
                project.dependencies.add(kspConfiguration, "$GROUP:kmp-swift-mockable-ksp:$version")
            }
            if (!isMacOS) return@configureEach

            target.binaries.withType(Framework::class.java).configureEach { framework ->
                val generate = registerGenerateTask(project, target, framework, appleTarget, extension)
                hostCandidates += framework to generate
            }
        }
    }

    private fun registerGenerateTask(
        project: Project,
        target: KotlinNativeTarget,
        framework: Framework,
        appleTarget: AppleTarget,
        extension: KmpSwiftMockableExtension,
    ): TaskProvider<GenerateSwiftMocks> {
        val name = "generateSwiftMocks" + framework.name.replaceFirstChar(Char::uppercaseChar) +
            target.name.replaceFirstChar(Char::uppercaseChar)
        val kspTaskName = "kspKotlin" + target.name.replaceFirstChar(Char::uppercaseChar)
        val generate = project.tasks.register(name, GenerateSwiftMocks::class.java) { task ->
            task.group = TASK_GROUP
            task.description = "Generates Swift mocks from the ${framework.name} of ${target.name}."
            task.dependsOn(framework.linkTaskProvider)
            task.dependsOn(project.tasks.matching { it.name == kspTaskName })
            task.framework.fileProvider(framework.linkTaskProvider.flatMap { it.outputFile })
            task.interfaceLists.from(
                project.layout.buildDirectory.file(
                    "generated/ksp/${target.name}/${target.name}Main/resources/kmp-swift-mockable/interfaces.json",
                ),
            )
            task.targetTriple.set(appleTarget.triple)
            task.sdk.set(appleTarget.sdk)
            task.condition.set(extension.condition)
            task.accessLevel.set(extension.accessLevel)
            task.generatorExecutable.set(extension.generatorExecutable)
            task.generatedFile.set(
                project.layout.buildDirectory.file("kmp-swift-mockable/${target.name}/${framework.name}/${framework.baseName}Mocks.swift"),
            )
            task.destinationFile.set(extension.outputDir.file("${framework.baseName}Mocks.swift"))
        }
        framework.linkTaskProvider.configure { it.finalizedBy(generate) }
        return generate
    }

    /** The task generating from the host simulator's debug framework, or else from the first Apple framework. */
    private fun hostTask(candidates: List<Pair<Framework, TaskProvider<GenerateSwiftMocks>>>): TaskProvider<GenerateSwiftMocks>? {
        val hostSimulator = if (System.getProperty("os.arch") == "aarch64") "ios_simulator_arm64" else "ios_x64"
        return candidates.firstOrNull { (framework, _) ->
            framework.buildType == NativeBuildType.DEBUG && framework.target.konanTarget.name == hostSimulator
        }?.second ?: candidates.firstOrNull()?.second
    }

    private companion object {
        const val GROUP = "io.github.yysskk"
        const val TASK_GROUP = "kmp-swift-mockable"
        const val KOTLIN_MULTIPLATFORM_PLUGIN = "org.jetbrains.kotlin.multiplatform"
        const val KSP_PLUGIN = "com.google.devtools.ksp"

        /** The plugin's version, which is also the version of the artifacts it adds. */
        val version: String by lazy {
            val properties = Properties()
            KmpSwiftMockablePlugin::class.java.getResourceAsStream("/kmp-swift-mockable.properties")!!.use(properties::load)
            properties.getProperty("version")
        }
    }
}
