package org.jetbrains.kotlin.gradle

import com.google.common.io.Files
import com.intellij.openapi.util.io.FileUtil
import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.util.createGradleCommand
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import java.io.File
import kotlin.test.*

private val SYSTEM_LINE_SEPARATOR = System.getProperty("line.separator")

abstract class BaseGradleIT {

    protected var workingDir = File(".")

    protected open fun defaultBuildOptions(): BuildOptions = BuildOptions(withDaemon = false)

    @Before
    fun setUp() {
        workingDir = Files.createTempDir()
    }

    @After
    fun tearDown() {
        FileUtil.delete(workingDir)
    }

    companion object {

        protected val ranDaemonVersions = hashMapOf<String, Int>()
        val resourcesRootFile = File("src/test/resources")
        val MAX_DAEMON_RUNS = 30

        @AfterClass
        @JvmStatic
        @Synchronized
        @Suppress("unused")
        fun tearDownAll() {
            ranDaemonVersions.keys.forEach { stopDaemon(it) }
            ranDaemonVersions.clear()
        }

        fun stopDaemon(ver: String) {
            println("Stopping gradle daemon v$ver")
            val wrapperDir = File(resourcesRootFile, "GradleWrapper-$ver")
            val cmd = createGradleCommand(arrayListOf("-stop"))
            val result = runProcess(cmd, wrapperDir)
            assert(result.isSuccessful) { "Could not stop daemon: $result" }
        }

        @Synchronized
        fun prepareDaemon(version: String) {
            val useCount = ranDaemonVersions.get(version)
            if (useCount == null || useCount > MAX_DAEMON_RUNS) {
                stopDaemon(version)
                ranDaemonVersions.put(version, 1)
            } else {
                ranDaemonVersions.put(version, useCount + 1)
            }
        }
    }

    // the second parameter is for using with ToolingAPI, that do not like --daemon/--no-daemon  options at all
    data class BuildOptions(val withDaemon: Boolean = false, val daemonOptionSupported: Boolean = true)

    open inner class Project(val projectName: String, val wrapperVersion: String = "1.4", val minLogLevel: LogLevel = LogLevel.DEBUG) {
        open val projectOriginalDir = File(resourcesRootFile, "testProject/$projectName")
        val projectWorkingDir = File(workingDir.canonicalFile, projectName)
        val wrapperDir = File(resourcesRootFile, "GradleWrapper-$wrapperVersion")

        open fun setupWorkingDir() {
            FileUtil.copyDir(projectOriginalDir, projectWorkingDir)
            FileUtil.copyDir(wrapperDir, projectWorkingDir)
        }
    }

    fun Project.build(vararg tasks: String, options: BuildOptions = defaultBuildOptions(), check: CompiledProject.() -> Unit) {
        val cmd = createBuildCommand(tasks, options)

        if (options.withDaemon) {
            prepareDaemon(wrapperVersion)
        }

        println("<=== Test build: ${this.projectName} $cmd ===>")

        runAndCheck(cmd, check)
    }

    private fun Project.runAndCheck(cmd: List<String>, check: CompiledProject.() -> Unit) {
        val projectDir = File(workingDir, projectName)
        if (!projectDir.exists())
            setupWorkingDir()

        val result = runProcess(cmd, projectDir)
        CompiledProject(this, result.stdout, result.exitCode).check()
    }

    private fun Project.createBuildCommand(params: Array<out String>, options: BuildOptions): List<String> =
            createGradleCommand(createGradleTailParameters(options, params))

    protected fun Project.createGradleTailParameters(options: BuildOptions, params: Array<out String> = arrayOf()): List<String> =
            params.asList() +
                    listOf("-PpathToKotlinPlugin=" + File("local-repo").absolutePath,
                            if (options.daemonOptionSupported)
                                if (options.withDaemon) "--daemon"
                                else "--no-daemon"
                            else null,
                            "--stacktrace",
                            "--${minLogLevel.name.toLowerCase()}",
                            "-Pkotlin.gradle.test=true")
                            .filterNotNull()
}

class CompiledProject(private val project: BaseGradleIT.Project, private val buildLog: String, protected val resultCode: Int) {
    companion object {
        private val kotlinSourcesListRegex = Regex("\\[KOTLIN\\] compile iteration: ([^\\r\\n]*)")
        private val javaSourcesListRegex = Regex("\\[DEBUG\\] \\[[^\\]]*JavaCompiler\\] Compiler arguments: ([^\\r\\n]*)")
    }

    private val compiledSources: Iterable<String> by lazy {
        fun extractAll(regex: Regex) = regex.findAll(buildLog).map { it.groups[1]!!.value }

        val kotlinFiles = extractAll(kotlinSourcesListRegex)
                .flatMap { it.splitToSequence(", ") }
                .map { File(project.projectWorkingDir, it) }
        val javaFiles = extractAll(javaSourcesListRegex)
                .flatMap { it.splitToSequence(" ") }
                .filter { it.endsWith(".java", ignoreCase = true) }
                .map { File(it) }
        val allFiles = (kotlinFiles + javaFiles).map { it.canonicalFile }
        allFiles.map { it.toRelativeString(project.projectWorkingDir) }.toList()
    }

    fun assertSuccessful(): CompiledProject {
        assertEquals(0, resultCode, "Gradle build failed")
        return this
    }

    fun assertFailed(): CompiledProject {
        assertNotEquals(0, resultCode, "Expected that Gradle build failed")
        return this
    }

    fun assertContains(vararg expected: String): CompiledProject {
        for (str in expected) {
            assertTrue(buildLog.contains(str.normalize()), "Should contain '$str', actual output: $buildLog")
        }
        return this
    }

    fun assertNotContains(vararg expected: String): CompiledProject {
        for (str in expected) {
            assertFalse(buildLog.contains(str.normalize()), "Should not contain '$str', actual output: $buildLog")
        }
        return this
    }


    fun assertReportExists(pathToReport: String = ""): CompiledProject {
        assertTrue(fileInWorkingDir(pathToReport).exists(), "The report [$pathToReport] does not exist.")
        return this
    }

    fun assertFileExists(path: String = ""): CompiledProject {
        assertTrue(fileInWorkingDir(path).exists(), "The file [$path] does not exist.")
        return this
    }

    fun assertNoSuchFile(path: String = ""): CompiledProject {
        assertFalse(fileInWorkingDir(path).exists(), "The file [$path] exists.")
        return this
    }

    fun assertFileContains(path: String, vararg expected: String): CompiledProject {
        val text = fileInWorkingDir(path).readText()
        expected.forEach {
            assertTrue(text.contains(it), "$path should contain '$it', actual file contents:\n$text")
        }
        return this
    }

    fun assertCompiledSources(expected: Iterable<String>, allowExtraCompiledFiles: Boolean): CompiledProject {
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

        return this
    }

    private fun fileInWorkingDir(path: String) =
            File(project.projectWorkingDir, path)

    private fun String.normalize() =
            lineSequence().joinToString(SYSTEM_LINE_SEPARATOR)

}