package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.BaseGradleIT
import java.io.File
import kotlin.test.*

class CompiledProject(val project: BaseGradleIT.Project, val buildLog: String, val resultCode: Int) {
    companion object {
        private val KOTLIN_SOURCES_LIST_REGEX = Regex("\\[KOTLIN\\] compile iteration: ([^\\r\\n]*)")
        private val JAVA_SOURCES_LIST_REGEX = Regex("\\[DEBUG\\] \\[[^\\]]*JavaCompiler\\] Compiler arguments: ([^\\r\\n]*)")
        private val SYSTEM_LINE_SEPARATOR = System.getProperty("line.separator")
    }

    private val compiledSources: Iterable<String> by lazy {
        fun extractAll(regex: Regex) = regex.findAll(buildLog).map { it.groups[1]!!.value }

        val kotlinFiles = extractAll(KOTLIN_SOURCES_LIST_REGEX)
                .flatMap { it.splitToSequence(", ") }
                .map { File(project.projectWorkingDir, it) }
        val javaFiles = extractAll(JAVA_SOURCES_LIST_REGEX)
                .flatMap { it.splitToSequence(" ") }
                .filter { it.endsWith(".java", ignoreCase = true) }
                .map { File(it) }
        val allFiles = (kotlinFiles + javaFiles).map { it.canonicalFile }
        allFiles.map { it.toRelativeString(project.projectWorkingDir) }.toList()
    }

    fun assertSuccessful() {
        assertEquals(0, resultCode, "Gradle build failed")
    }

    fun assertFailed() {
        assertNotEquals(0, resultCode, "Expected that Gradle build failed")
    }

    fun assertContains(vararg expected: String) {
        for (str in expected) {
            assertTrue(buildLog.contains(str.normalize()), "Should contain '$str', actual output: $buildLog")
        }
    }

    fun assertNotContains(vararg expected: String) {
        for (str in expected) {
            assertFalse(buildLog.contains(str.normalize()), "Should not contain '$str', actual output: $buildLog")
        }
    }

    fun assertReportExists(pathToReport: String = "") {
        assertTrue(fileInWorkingDir(pathToReport).exists(), "The report [$pathToReport] does not exist.")
    }

    fun assertFileExists(path: String = "") {
        assertTrue(fileInWorkingDir(path).exists(), "The file [$path] does not exist.")
    }

    fun assertNoSuchFile(path: String = "") {
        assertFalse(fileInWorkingDir(path).exists(), "The file [$path] exists.")
    }

    fun assertFileContains(path: String, vararg expected: String) {
        val text = fileInWorkingDir(path).readText()
        expected.forEach {
            assertTrue(text.contains(it), "$path should contain '$it', actual file contents:\n$text")
        }
    }

    fun assertCompiledSources(expected: Iterable<String>, allowExtraCompiledFiles: Boolean) {
        val expectedSet = expected.toSortedSet()
        val actualSet = compiledSources.toSortedSet()

        val extraFiles = if (!allowExtraCompiledFiles) actualSet - expectedSet else emptySet()
        val missingFiles = expectedSet - actualSet

        val missingFilesMsg = "Expected to be compiled: $missingFiles\n"
        val extraFilesMsg = "Not expected to be compiled: $extraFiles\n"

        if (missingFiles.isNotEmpty() && extraFiles.isNotEmpty()) {
            fail(missingFilesMsg + extraFilesMsg)
        }
        else if (missingFiles.isNotEmpty()) {
            fail(missingFilesMsg)
        }
        else if (extraFiles.isNotEmpty()) {
            fail(extraFilesMsg)
        }
    }

    private fun fileInWorkingDir(path: String) =
            File(project.projectWorkingDir, path)

    private fun String.normalize() =
            lineSequence().joinToString(SYSTEM_LINE_SEPARATOR)
}