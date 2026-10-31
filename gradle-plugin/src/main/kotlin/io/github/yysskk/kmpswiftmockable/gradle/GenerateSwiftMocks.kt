package io.github.yysskk.kmpswiftmockable.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/**
 * Writes `@Mockable` Swift protocols mirroring the annotated interfaces of one linked
 * Apple framework.
 *
 * The generator is the one bundled with the plugin unless [generatorExecutable] is set.
 * The task writes [generatedFile], its own output in the build directory, and copies it
 * to [destinationFile], which every framework of a module shares. The copy reflects the
 * framework linked last, which is the one the Swift code is about to compile against.
 * The task reads the framework's headers and module map rather than its binary, so
 * relinking without an API change leaves it up to date, as long as the copy is current.
 * It is not cacheable, since a cache hit would skip the copy.
 */
@DisableCachingByDefault(because = "The task copies its output to a file outside the build directory.")
public abstract class GenerateSwiftMocks @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    /** The linked `.framework` bundle. */
    @get:Internal
    public abstract val framework: DirectoryProperty

    /** The framework's `Headers` directory, which describes its Objective-C interface. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public val headers: Provider<Directory> get() = framework.dir("Headers")

    /** The framework's `Modules` directory, which holds its module map. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public val modules: Provider<Directory> get() = framework.dir("Modules")

    /** The interface lists written by the symbol processor; missing files are ignored. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val interfaceLists: ConfigurableFileCollection

    /** The target triple the framework was built for. */
    @get:Input
    public abstract val targetTriple: Property<String>

    /** The SDK the framework was built against, e.g. `iphonesimulator`. */
    @get:Input
    public abstract val sdk: Property<String>

    /** The compilation condition of the generated mocks. */
    @get:Input
    @get:Optional
    public abstract val condition: Property<String>

    /** The access level of the generated protocols. */
    @get:Input
    public abstract val accessLevel: Property<SwiftAccessLevel>

    /** The generator to run instead of the one bundled with the plugin. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val generatorExecutable: RegularFileProperty

    /** The plugin's version, which identifies the bundled generator. */
    @get:Input
    public abstract val pluginVersion: Property<String>

    /** Where the bundled generator is extracted to. */
    @get:Internal
    public abstract val generatorCacheDirectory: DirectoryProperty

    /** The Swift file the generator writes, which belongs to this task alone. */
    @get:OutputFile
    public abstract val generatedFile: RegularFileProperty

    /** Where the generated file is copied to: `<outputDir>/<FrameworkBaseName>Mocks.swift`. */
    @get:Internal
    public abstract val destinationFile: RegularFileProperty

    init {
        // Another framework's task may have replaced the shared copy since this one ran.
        outputs.upToDateWhen { task -> (task as GenerateSwiftMocks).isDestinationCurrent() }
    }

    @TaskAction
    internal fun generate() {
        execOperations.exec { it.commandLine(generatorCommandLine()) }
        copyToDestination()
    }

    /** Copies the generated file unless the destination already holds it, to keep its date. */
    internal fun copyToDestination() {
        if (isDestinationCurrent()) return
        val destination = destinationFile.get().asFile
        destination.parentFile.mkdirs()
        generatedFile.get().asFile.copyTo(destination, overwrite = true)
    }

    private fun isDestinationCurrent(): Boolean {
        val generated = generatedFile.get().asFile
        val destination = destinationFile.get().asFile
        return generated.isFile && destination.isFile && generated.readBytes().contentEquals(destination.readBytes())
    }

    /** The generator's command line for the task's current inputs. */
    internal fun generatorCommandLine(): List<String> {
        val generator = generatorExecutable.orNull?.asFile
            ?: BundledGenerator.extract(generatorCacheDirectory.get().asFile)
        return buildList {
            add(generator.absolutePath)
            addAll(listOf("--framework", framework.get().asFile.absolutePath))
            addAll(listOf("--target", targetTriple.get()))
            addAll(listOf("--sdk", sdk.get()))
            interfaceLists.files.filter { it.isFile }.sortedBy { it.path }.forEach {
                addAll(listOf("--interfaces", it.absolutePath))
            }
            condition.orNull?.let { addAll(listOf("--condition", it)) }
            addAll(listOf("--access-level", accessLevel.get().swiftKeyword))
            addAll(listOf("--output", generatedFile.get().asFile.absolutePath))
        }
    }
}
