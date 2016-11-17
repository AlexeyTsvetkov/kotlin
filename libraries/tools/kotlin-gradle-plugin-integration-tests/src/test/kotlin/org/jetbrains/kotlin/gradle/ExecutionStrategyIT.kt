package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.junit.Test

class ExecutionStrategyIT(): BaseGradleIT() {
    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

    @Test
    fun testDaemon() {
        doTestExecutionStrategy("daemon")
    }

    @Test
    fun testInProcess() {
        doTestExecutionStrategy("in-process")
    }

    @Test
    fun testOutOfProcess() {
        doTestExecutionStrategy("out-of-process")
    }

    private fun doTestExecutionStrategy(executionStrategy: String) {
        val project = Project("kotlinJavaProject", GRADLE_VERSION)
        val strategyCLIArg = "-Dkotlin.compiler.execution.strategy=$executionStrategy"
        val finishMessage = "Finished executing kotlin compiler using $executionStrategy strategy"

        project.build("build", strategyCLIArg) {
            assertSuccessful()
            assertContains(finishMessage)
            assertFileExists("build/classes/main/demo/KotlinGreetingJoiner.class")
        }

        val greeterJava = project.projectDir.getFileByName("Greeter.java")
        greeterJava.delete()
        project.build("build", strategyCLIArg) {
            assertFailed()
            assertContains(finishMessage)
            assert(output.contains("Unresolved reference: Greeter", ignoreCase = true))
        }
    }

}