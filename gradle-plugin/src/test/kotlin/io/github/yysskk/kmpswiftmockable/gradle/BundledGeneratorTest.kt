package io.github.yysskk.kmpswiftmockable.gradle

import org.gradle.api.GradleException
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BundledGeneratorTest {
    private val cacheDirectory: File = Files.createTempDirectory("generator-cache").toFile()

    @AfterTest
    fun deleteCache() {
        cacheDirectory.deleteRecursively()
    }

    private fun extract(contents: String): File =
        BundledGenerator.extract(cacheDirectory) { contents.byteInputStream() }

    @Test
    fun `the generator is extracted as an executable`() {
        val executable = extract("binary")

        assertEquals("kmp-swift-mockable-generator", executable.name)
        assertEquals("binary", executable.readText())
        assertTrue(executable.canExecute())
        assertTrue(executable.startsWith(cacheDirectory))
    }

    @Test
    fun `an extracted generator is reused`() {
        val first = extract("binary")
        val modified = first.lastModified() - 10_000
        first.setLastModified(modified)

        val second = extract("binary")
        assertEquals(first, second)
        assertEquals(modified, second.lastModified())
    }

    @Test
    fun `a different generator is extracted beside the previous one`() {
        val first = extract("one")
        val second = extract("two")

        assertNotEquals(first, second)
        assertEquals("one", first.readText())
        assertEquals("two", second.readText())
    }

    @Test
    fun `a copy that lost its executable bit is replaced`() {
        val first = extract("binary")
        first.setExecutable(false)

        assertTrue(extract("binary").canExecute())
    }

    @Test
    fun `a plugin built without the generator asks for one`() {
        val error = assertFailsWith<GradleException> { BundledGenerator.extract(cacheDirectory) { null } }
        assertTrue(error.message!!.contains("kmpSwiftMockable.generatorExecutable"))
    }
}
