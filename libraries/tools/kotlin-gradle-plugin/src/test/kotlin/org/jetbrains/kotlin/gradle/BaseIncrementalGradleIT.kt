package org.jetbrains.kotlin.gradle

import com.intellij.openapi.util.io.FileUtil
import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.util.BuildStep
import org.jetbrains.kotlin.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.util.parseTestBuildLog
import org.jetbrains.kotlin.incremental.testingUtils.*
import org.junit.Assume
import java.io.File

abstract class BaseIncrementalGradleIT : BaseGradleIT() {

    inner class JpsTestProject(
            val buildLogFinder: BuildLogFinder,
            val resourcesBase: File,
            val relPath: String,
            wrapperVersion: GradleVersion = GradleVersion.`2-10`,
            minLogLevel: LogLevel = LogLevel.DEBUG,
            val allowExtraCompiledFiles: Boolean = false
    ) : Project(File(relPath).name, wrapperVersion, minLogLevel) {
        override val projectOriginalDir = File(resourcesBase, relPath)
        val mapWorkingToOriginalFile = hashMapOf<File, File>()

        override fun setupWorkingDir() {
            super.setupWorkingDir()

            val srcDir = File(projectWorkingDir, "src")
            srcDir.mkdirs()
            val sourceMapping = copyTestSources(projectOriginalDir, srcDir, filePrefix = "")
            mapWorkingToOriginalFile.putAll(sourceMapping)

            FileUtil.copyDir(File(resourcesRootFile, "incrementalGradleProject"), projectWorkingDir)
        }
    }

    fun JpsTestProject.performAndAssertBuildStages(options: BuildOptions = defaultBuildOptions()) {
        // TODO: support multimodule tests
        if (projectOriginalDir.walk().filter { it.name.equals("dependencies.txt", ignoreCase = true) }.any()) {
            Assume.assumeTrue("multimodule tests are not supported yet", false)
        }

        build("build", options = options) {
            assertSuccessful()
            assertReportExists()
        }

        val buildLogFile = buildLogFinder.findBuildLog(projectOriginalDir) ?:
                throw IllegalStateException("build log file not found in $projectOriginalDir")
        val buildLogSteps = parseTestBuildLog(buildLogFile)
        val modifications = getModificationsToPerform(projectOriginalDir,
                                                      moduleNames = null,
                                                      allowNoFilesWithSuffixInTestData = false,
                                                      touchPolicy = TouchPolicy.CHECKSUM)

        assert(modifications.size == buildLogSteps.size) {
            "Modifications count (${modifications.size}) != expected build log steps count (${buildLogSteps.size})"
        }

        var i = 1
        for ((modificationStep, expected) in modifications.zip(buildLogSteps)) {
            println("<--- Test modification step #${i++}: " +
                    "Expecting build to ${if (expected.compileSucceeded) "succeed" else "fail"}; " +
                    "Expecting source files to be compiled: ${(expected.compiledKotlinFiles + expected.compiledJavaFiles).toSortedSet()} --->")

            modificationStep.forEach { it.perform(projectWorkingDir, mapWorkingToOriginalFile) }
            buildAndAssertStageResults(expected)
        }

        rebuildAndCompareOutput(rebuildSucceedExpected = buildLogSteps.last().compileSucceeded)
    }

    private fun JpsTestProject.buildAndAssertStageResults(expected: BuildStep, options: BuildOptions = defaultBuildOptions()) {
        build("build", options = options) {
            if (expected.compileSucceeded) {
                assertSuccessful()
                assertCompiledSources(expected.compiledKotlinFiles + expected.compiledJavaFiles, allowExtraCompiledFiles)
            }
            else {
                assertFailed()
            }
        }
    }

    private fun JpsTestProject.rebuildAndCompareOutput(rebuildSucceedExpected: Boolean) {
        val outDir = File(File(projectWorkingDir, "build"), "classes")
        val incrementalOutDir = File(workingDir, "kotlin-classes-incremental")
        FileUtil.copyDir(outDir, incrementalOutDir)

        build("clean", "build") {
            if (rebuildSucceedExpected) assertSuccessful() else assertFailed()
            outDir.mkdirs()
            assertEqualDirectories(outDir, incrementalOutDir, forgiveExtraFiles = !rebuildSucceedExpected)
        }
    }
}

