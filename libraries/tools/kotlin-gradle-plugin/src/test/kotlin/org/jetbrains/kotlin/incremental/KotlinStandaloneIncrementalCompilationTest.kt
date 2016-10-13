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

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.incremental.BuildStep
import org.jetbrains.kotlin.gradle.incremental.parseTestBuildLog
import org.jetbrains.kotlin.incremental.testingUtils.*
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(Parameterized::class)
class KotlinStandaloneIncrementalCompilationTest : TestWithWorkingDir() {
    @Parameterized.Parameter
    lateinit var testDir: File

    @Suppress("unused")
    @Parameterized.Parameter(value = 1)
    lateinit var readableName: String

    @Test
    fun testFromJps() {
        val srcDir = File(workingDir, "src").apply { mkdirs() }
        val cacheDir = File(workingDir, "incremental-data").apply { mkdirs() }
        val outDir = File(workingDir, "out").apply { mkdirs() }

        val mapWorkingToOriginalFile = HashMap(copyTestSources(testDir, srcDir, filePrefix = ""))
        val sourceRoots = listOf(srcDir)
        val args = K2JVMCompilerArguments()
        args.destinationAsFile = outDir
        args.moduleName = testDir.name
        args.classpath = ""


        data class CompilationResult(val exitCode: ExitCode, val compiledSources: Iterable<File>)
        fun make(): CompilationResult {
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

            makeIncrementally(cacheDir, sourceRoots, args, reporter = reporter)
            return CompilationResult(resultExitCode, compiledSources)
        }

        // initial build
        make()

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

        println("<--- Expected build log size: ${buildLogSteps.size}")
        buildLogSteps.forEach {
            println("<--- Expected build log stage: ${if (it.compileSucceeded) "succeeded" else "failed"}: kotlin: ${it.compiledKotlinFiles} java: ${it.compiledJavaFiles}")
        }

        for ((modificationStep, buildLogStep) in modifications.zip(buildLogSteps)) {
            modificationStep.forEach { it.perform(workingDir, mapWorkingToOriginalFile) }
            val (exitCode, compiledSources) = make()

            if (buildLogStep.compileSucceeded) {
                assertEquals(ExitCode.OK, exitCode, "Exit code")
            }
            else {
                assertNotEquals(ExitCode.OK, exitCode, "Exit code")
            }

            val expectedSources = buildLogStep.compiledKotlinFiles.toTypedArray()
            val d = 0
        }
        val c = 0

        //val originalDir = File(jpsResourcesPath, relativePath)
        //JpsTestProject(buildLogFinder, jpsResourcesPath, relativePath).performAndAssertBuildStages(weakTesting = true)
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
        fun data(): List<Array<*>> =
                jpsResourcesPath.walk()
                        .onEnter { it !in ignoredDirs }
                        .filter { it.isDirectory && buildLogFinder.findBuildLog(it) != null && "dependencies.txt" !in it.list() }
                        .map { arrayOf(it, it.relativeTo(it.parentFile.parentFile).path) }
                        .toList()
    }

    /*inner class JpsTestProject(val buildLogFinder: BuildLogFinder, val resourcesBase: File, val relPath: String, wrapperVersion: String = "2.10", minLogLevel: LogLevel = LogLevel.DEBUG) : Project(File(relPath).name, wrapperVersion, null, minLogLevel) {
        override val resourcesRoot = File(resourcesBase, relPath)
        val mapWorkingToOriginalFile = hashMapOf<File, File>()

        override fun setupWorkingDir() {
            val srcDir = File(projectDir, "src")
            srcDir.mkdirs()
            val sourceMapping = copyTestSources(resourcesRoot, srcDir, filePrefix = "")
            mapWorkingToOriginalFile.putAll(sourceMapping)
        }
    }*/

    /*fun JpsTestProject.performAndAssertBuildStages(options: BuildOptions = defaultBuildOptions(), weakTesting: Boolean = false) {
        // TODO: support multimodule tests
        if (resourcesRoot.walk().filter { it.name.equals("dependencies.txt", ignoreCase = true) }.any()) {
            Assume.assumeTrue("multimodule tests are not supported yet", false)
        }

        build("build", options = options) {
            assertSuccessful()
            assertReportExists()
        }

        val buildLogFile = buildLogFinder.findBuildLog(resourcesRoot) ?:
                throw IllegalStateException("build log file not found in $resourcesRoot")
        val buildLogSteps = parseTestBuildLog(buildLogFile)
        val modifications = getModificationsToPerform(resourcesRoot,
                moduleNames = null,
                allowNoFilesWithSuffixInTestData = false,
                touchPolicy = TouchPolicy.CHECKSUM)

        assert(modifications.size == buildLogSteps.size) {
            "Modifications count (${modifications.size}) != expected build log steps count (${buildLogSteps.size})"
        }

        println("<--- Expected build log size: ${buildLogSteps.size}")
        buildLogSteps.forEach {
            println("<--- Expected build log stage: ${if (it.compileSucceeded) "succeeded" else "failed"}: kotlin: ${it.compiledKotlinFiles} java: ${it.compiledJavaFiles}")
        }

        for ((modificationStep, buildLogStep) in modifications.zip(buildLogSteps)) {
            modificationStep.forEach { it.perform(projectDir, mapWorkingToOriginalFile) }
            buildAndAssertStageResults(buildLogStep, weakTesting = weakTesting)
        }

        rebuildAndCompareOutput(rebuildSucceedExpected = buildLogSteps.last().compileSucceeded)
    }

    private fun JpsTestProject.buildAndAssertStageResults(expected: BuildStep, options: BuildOptions = defaultBuildOptions(), weakTesting: Boolean = false) {
        build("build", options = options) {
            if (expected.compileSucceeded) {
                assertSuccessful()
                assertCompiledJavaSources(expected.compiledJavaFiles, weakTesting)
                assertCompiledKotlinSources(expected.compiledKotlinFiles, weakTesting)
            }
            else {
                assertFailed()
            }
        }
    }

    private fun JpsTestProject.rebuildAndCompareOutput(rebuildSucceedExpected: Boolean) {
        val outDir = File(File(projectDir, "build"), "classes")
        val incrementalOutDir = File(workingDir, "kotlin-classes-incremental")
        incrementalOutDir.mkdirs()
        copyDirRecursively(outDir, incrementalOutDir)

        build("clean", "build") {
            val rebuildSucceed = resultCode == 0
            assertEquals(rebuildSucceed, rebuildSucceedExpected, "Rebuild exit code differs from incremental exit code")
            outDir.mkdirs()
            assertEqualDirectories(outDir, incrementalOutDir, forgiveExtraFiles = !rebuildSucceed)
        }
    }*/
}

