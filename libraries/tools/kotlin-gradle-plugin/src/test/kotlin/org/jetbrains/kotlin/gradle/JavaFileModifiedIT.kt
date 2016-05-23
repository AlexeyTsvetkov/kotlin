package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class JavaFileModifiedIT : BaseGradleIT() {
    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

    @Test
    fun testRemoveJavaNonIC() {
        doTestRemoveDependency(defaultBuildOptions().copy())
    }

    @Test
    fun testRemoveJavaIC() {
        doTestRemoveDependency(defaultBuildOptions().copy(incremental = true))
    }

    private fun doTestRemoveDependency(options: BuildOptions) {
        val project = Project("kotlinProject", GRADLE_VERSION)
        project.setupWorkingDir()

        val buildGradle = project.projectDir.getFileByName("build.gradle")
        val dependency = "compile 'com.google.guava:guava:12.0'"
        assert(dependency in buildGradle.readText()) { "${buildGradle.name} should contain \"$dependency\"" }

        project.build("build", options = options) {
            assertSuccessful()
        }

        buildGradle.modify { it.replace(dependency, "") }

        project.build("build", options = options) {
            assertFailed()
            assertCompiledKotlinSources(project.relativize(File(project.projectDir, "src/main").allKotlinFiles()))
        }
    }
}