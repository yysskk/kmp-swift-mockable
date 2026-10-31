package io.github.yysskk.kmpswiftmockable.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
 * The task reads the framework's headers and module map rather than its binary, so
 * relinking without an API change leaves it up to date. It is not cacheable: every
 * framework of a module writes the same file, outside the build directory.
 */
@DisableCachingByDefault(because = "Several tasks write the same file outside the build directory.")
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

    /** The generator to run. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val generatorExecutable: RegularFileProperty

    /** The Swift file to write. */
    @get:OutputFile
    public abstract val outputFile: RegularFileProperty

    @TaskAction
    internal fun generate() {
        execOperations.exec { it.commandLine(generatorCommandLine()) }
    }

    /** The generator's command line for the task's current inputs. */
    internal fun generatorCommandLine(): List<String> {
        val generator = generatorExecutable.orNull?.asFile ?: throw GradleException(
            "kmp-swift-mockable has no generator to run. Set kmpSwiftMockable.generatorExecutable.",
        )
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
            addAll(listOf("--output", outputFile.get().asFile.absolutePath))
        }
    }
}
