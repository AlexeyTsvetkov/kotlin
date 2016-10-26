/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.gradle.incremental.parseTestBuildLog
import org.jetbrains.kotlin.incremental.testingUtils.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.*
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class KotlinStandaloneIncrementalCompilationTest : TestWithWorkingDir() {
    @Parameterized.Parameter
    lateinit var testDir: File

    @Suppress("unused")
    @Parameterized.Parameter(value = 1)
    lateinit var readableName: String

    private var isEnabledBackup: Boolean = false
    private var isExperimentalBackup: Boolean = false

    @Before
    override fun setUp() {
        super.setUp()
        isEnabledBackup = IncrementalCompilation.isEnabled()
        isExperimentalBackup = IncrementalCompilation.isExperimental()
        IncrementalCompilation.setIsEnabled(true)
        IncrementalCompilation.setIsExperimental(true)
    }

    @After
    override fun tearDown() {
        IncrementalCompilation.setIsEnabled(isEnabledBackup)
        IncrementalCompilation.setIsExperimental(isExperimentalBackup)
        super.tearDown()
    }

    @Test
    fun testFromJps() {
        fun Iterable<File>.relativePaths() =
                map { it.relativeTo(workingDir).path.replace('\\', '/') }

        val srcDir = File(workingDir, "src").apply { mkdirs() }
        val cacheDir = File(workingDir, "incremental-data").apply { mkdirs() }
        val outDir = File(workingDir, "out").apply { mkdirs() }

        val mapWorkingToOriginalFile = HashMap(copyTestSources(testDir, srcDir, filePrefix = ""))
        val sourceRoots = listOf(srcDir)
        val args = K2JVMCompilerArguments()
        args.destination = outDir.path
        args.moduleName = testDir.name
        args.classpath = compileClasspath()
        // initial build
        make(cacheDir, sourceRoots, args)

        // modifications
        val buildLogFile = buildLogFinder.findBuildLog(testDir) ?: throw IllegalStateException("build log file not found in $workingDir")
        val buildLogSteps = parseTestBuildLog(buildLogFile)
        val modifications = getModificationsToPerform(testDir,
                moduleNames = null,
                allowNoFilesWithSuffixInTestData = false,
                touchPolicy = TouchPolicy.CHECKSUM)

        assert(modifications.size == buildLogSteps.size) {
            "Modifications count (${modifications.size}) != expected build log steps count (${buildLogSteps.size})"
        }

        val expectedSB = StringBuilder()
        val actualSB = StringBuilder()
        var step = 1
        for ((modificationStep, buildLogStep) in modifications.zip(buildLogSteps)) {
            modificationStep.forEach { it.perform(workingDir, mapWorkingToOriginalFile) }
            val (exitCode, compiledSources, compileErrors) = make(cacheDir, sourceRoots, args)

            val expectedStep = stepLogAsString(step, buildLogStep.compiledKotlinFiles, buildLogStep.compileErrors)
            expectedSB.appendLine(expectedStep)
            val actualStep = stepLogAsString(step, compiledSources.relativePaths(), compileErrors)
            actualSB.appendLine(actualStep)
            step++
        }

        assertEquals(expectedSB.toString(), actualSB.toString())
    }

    private fun compileClasspath(): String {
        val currentClasspath = System.getProperty("java.class.path").split(File.pathSeparator)
        val stdlib = currentClasspath.find { it.contains("kotlin-stdlib") }
        val runtime = currentClasspath.find { it.contains("kotlin-runtime") }
        return listOf(stdlib, runtime).joinToString(File.pathSeparator)
    }

    data class CompilationResult(val exitCode: ExitCode, val compiledSources: Iterable<File>, val compileErrors: Collection<String>)

    private fun make(cacheDir: File, sourceRoots: Iterable<File>, args: K2JVMCompilerArguments): CompilationResult {
        val compiledSources = arrayListOf<File>()
        var resultExitCode = ExitCode.OK

        val reporter = object : IncReporter() {
            override fun report(message: ()->String) {
            }

            override fun reportCompileIteration(sourceFiles: Iterable<File>, exitCode: ExitCode) {
                compiledSources.addAll(sourceFiles)
                resultExitCode = exitCode
            }
        }

        val messageCollector = ErrorMessageCollector()
        makeIncrementally(cacheDir, sourceRoots, args, reporter = reporter, messageCollector = messageCollector)
        return CompilationResult(resultExitCode, compiledSources, messageCollector.errors)
    }

    private fun stepLogAsString(step: Int, ktSources: Iterable<String>, errors: Collection<String>): String {
        val sb = StringBuilder()

        sb.appendLine("<======= STEP $step =======>")
        sb.appendLine()
        sb.appendLine("Compiled kotlin sources:")
        ktSources.toSet().toTypedArray().sortedArray().forEach { sb.appendLine(it) }
        sb.appendLine()

        if (errors.isEmpty()) {
            sb.appendLine("SUCCESS")
        }
        else {
            sb.appendLine("FAILURE")
            errors.filter(String::isNotEmpty).forEach { sb.appendLine(it) }
        }

        return sb.toString()
    }

    private fun StringBuilder.appendLine(line: String = "") {
        append(line)
        append('\n')
    }

    private class ErrorMessageCollector : MessageCollector {
        val errors = ArrayList<String>()

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
            if (severity.isError) {
                errors.add(message)
            }
        }

        override fun clear() {
            errors.clear()
        }

        override fun hasErrors(): Boolean =
                errors.isNotEmpty()
    }

    companion object {
        private val jpsResourcesPath = File("../../../jps-plugin/testData/incremental")
        private val ignoredDirs = setOf(File(jpsResourcesPath, "cacheVersionChanged"),
                                        File(jpsResourcesPath, "changeIncrementalOption"),
                                        File(jpsResourcesPath, "custom"),
                                        File(jpsResourcesPath, "lookupTracker"))
        private val buildLogFinder = BuildLogFinder(isExperimentalEnabled = true, isGradleEnabled = true)

        @Suppress("unused")
        @Parameterized.Parameters(name = "{1}")
        @JvmStatic
        fun data(): List<Array<*>> {
            fun File.isValidTestDir(): Boolean {
                if (!isDirectory) return false

                // multi-module tests
                if ("dependencies.txt" in list()) return false

                val logFile = buildLogFinder.findBuildLog(this) ?: return false
                val parsedLog = parseTestBuildLog(logFile)
                // tests with java may be expected to fail in javac
                return parsedLog.all { it.compiledJavaFiles.isEmpty() }
            }

            return jpsResourcesPath.walk()
                    .onEnter { it !in ignoredDirs }
                    .filter(File::isValidTestDir)
                    .map { arrayOf(it, it.relativeTo(it.parentFile.parentFile).path) }
                    .toList()
        }
    }
}

