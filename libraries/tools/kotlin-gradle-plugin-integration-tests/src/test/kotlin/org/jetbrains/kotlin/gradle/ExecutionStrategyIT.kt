package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class DaemonExecutionStrategyIT : BaseExecutionStrategyIT() {
    override val executionStrategy: String = "daemon"
}

class InProcessExecutionStrategyIT : BaseExecutionStrategyIT() {
    override val executionStrategy: String = "in-process"

    override fun checkExecutionStrategy(compiledProject: CompiledProject) {
        super.checkExecutionStrategy(compiledProject)
        compiledProject.assertContains(GradleCompilerRunner.jarClearSuccessMessage)
    }
}

class OutOfProcessStrategyIT : BaseExecutionStrategyIT() {
    override val executionStrategy: String = "out-of-process"
}

abstract class BaseExecutionStrategyIT : BaseGradleIT() {
    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

    abstract val executionStrategy: String

    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(executionStrategy = executionStrategy)
    }

    @Test
    fun testJvmCompile() {
        doTestCompile("app/build/classes/main/foo/MainKt.class")
    }

    @Test
    fun testJsCompile() {
        doTestCompile("app/build/classes/main/app.js", setupProject = { project ->
            val buildGradle = File(project.projectDir, "app/build.gradle")
            buildGradle.modify { it.replace("apply plugin: \"kotlin\"", "apply plugin: \"kotlin2js\"") }
        })
    }

    private fun doTestCompile(outputFilePath: String, setupProject: (Project) -> Unit = {}) {
        val project = Project("kotlinBuiltins", GRADLE_VERSION)
        project.setupWorkingDir()
        setupProject(project)

        project.build("build") {
            assertSuccessful()
            checkExecutionStrategy(this)
            assertNoWarnings()
            assertFileExists(outputFilePath)
        }

        val fKt = project.projectDir.getFileByName("f.kt")
        fKt.delete()
        project.build("build") {
            assertFailed()
            assert(output.contains("Unresolved reference: f", ignoreCase = true))
            checkExecutionStrategy(this)
        }
    }

    protected open fun checkExecutionStrategy(compiledProject: CompiledProject) {
        val finishMessage = "Finished executing kotlin compiler using $executionStrategy strategy"
        compiledProject.assertContains(finishMessage)
    }
}