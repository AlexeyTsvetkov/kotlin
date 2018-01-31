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
    abstract val executionStrategy: String

    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(executionStrategy = executionStrategy)
    }

    @Test
    fun testJvmCompile() {
        doTestCompile(checkOutput = {
            assertFileExists(kotlinClassesDir(subproject = "app") + "/foo/MainKt.class")
        })
    }

    @Test
    fun testJsCompile() {
        doTestCompile(checkOutput = {
            assertFileExists(kotlinClassesDir(subproject = "app") + "/app.js")
        }, setupProject = { project ->
            val buildGradle = File(project.projectDir, "app/build.gradle")
            buildGradle.modify { it.replace("apply plugin: \"kotlin\"", "apply plugin: \"kotlin2js\"") }
        })
    }

    private fun doTestCompile(checkOutput: CompiledProject.() -> Unit, setupProject: (Project) -> Unit = {}) {
        val project = Project("kotlinBuiltins", GradleVersionRequired.None)
        project.setupWorkingDir()
        setupProject(project)

        project.build("build") {
            assertSuccessful()
            checkExecutionStrategy(this)
            assertNoWarnings()
            checkOutput()
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