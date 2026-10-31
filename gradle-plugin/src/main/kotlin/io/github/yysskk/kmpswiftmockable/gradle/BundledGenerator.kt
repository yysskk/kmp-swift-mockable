package io.github.yysskk.kmpswiftmockable.gradle

import org.gradle.api.GradleException
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * The `kmp-swift-mockable-generator` executable shipped inside the plugin's jar, a
 * universal macOS binary.
 *
 * It is copied once to a cache directory, under a name derived from its contents, so
 * builds share one copy and a plugin update never runs a stale one.
 */
internal object BundledGenerator {
    /** The executable's path among the plugin's resources. */
    const val RESOURCE = "kmp-swift-mockable/generator/kmp-swift-mockable-generator"

    /**
     * Copies the executable to [cacheDirectory] unless it is already there, and returns it.
     *
     * @param open opens the bundled executable, or returns `null` when there is none.
     */
    fun extract(
        cacheDirectory: File,
        open: () -> InputStream? = { BundledGenerator::class.java.classLoader.getResourceAsStream(RESOURCE) },
    ): File {
        val bytes = open()?.use(InputStream::readBytes) ?: throw GradleException(
            "This build of the kmp-swift-mockable plugin has no bundled generator. " +
                "Set kmpSwiftMockable.generatorExecutable to a kmp-swift-mockable-generator executable.",
        )
        val directory = cacheDirectory.resolve(sha256(bytes))
        val executable = directory.resolve("kmp-swift-mockable-generator")
        if (!executable.isFile || !executable.canExecute()) {
            directory.mkdirs()
            // Write beside the final name and rename, so that a concurrent build never
            // runs a partly written file.
            val temporary = File.createTempFile("kmp-swift-mockable-generator", null, directory)
            temporary.writeBytes(bytes)
            temporary.setExecutable(true)
            if (!temporary.renameTo(executable) && !executable.isFile) {
                temporary.delete()
                throw GradleException("Could not extract the kmp-swift-mockable generator to $executable.")
            }
            temporary.delete()
        }
        return executable
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
