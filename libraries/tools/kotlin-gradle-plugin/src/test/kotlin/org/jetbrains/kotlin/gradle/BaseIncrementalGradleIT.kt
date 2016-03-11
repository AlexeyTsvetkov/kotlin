package org.jetbrains.kotlin.gradle

import com.intellij.openapi.util.io.FileUtil
import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.util.BuildStep
import org.jetbrains.kotlin.gradle.util.parseTestBuildLog
import org.jetbrains.kotlin.incremental.testingUtils.TouchPolicy
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import org.jetbrains.kotlin.incremental.testingUtils.copyTestSources
import org.jetbrains.kotlin.incremental.testingUtils.getModificationsToPerform
import org.junit.Assume
import java.io.File
import kotlin.test.assertNotNull

abstract class BaseIncrementalGradleIT : BaseGradleIT() {

    inner class JpsTestProject(
            val resourcesBase: File,
            val relPath: String, wrapperVersion: String = "2.10",
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

        val buildLogFile = projectOriginalDir.listFiles { f: File -> f.name.endsWith("build.log") }?.sortedBy { it.length() }?.firstOrNull()
        assertNotNull(buildLogFile, "*build.log file not found" )

        val buildLogSteps = parseTestBuildLog(buildLogFile!!)
        val modifications = getModificationsToPerform(projectOriginalDir,
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
            modificationStep.forEach { it.perform(projectWorkingDir, mapWorkingToOriginalFile) }
            buildAndAssertStageResults(buildLogStep)
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

