package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class DependencyChangedIT : BaseGradleIT() {
    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

    override fun defaultBuildOptions(): BuildOptions =
            BuildOptions(withDaemon = true)

    @Test
    fun testAddDependencyNonIC() {
        doTestAddDependency(defaultBuildOptions().copy())
    }

    @Test
    fun testAddDependencyIC() {
        doTestAddDependency(defaultBuildOptions().copy(incremental = true))
    }

    private fun doTestAddDependency(options: BuildOptions) {
        val project = Project("kotlinProject", GRADLE_VERSION)
        project.setupWorkingDir()

        val buildGradle = project.projectDir.getFileByName("build.gradle")
        val commentedOutDependency = "// compile 'joda-time:joda-time:2.9.3'"
        val dependency = commentedOutDependency.replace("// ", "")
        assert(commentedOutDependency in buildGradle.readText()) { "${buildGradle.name} should contain \"$commentedOutDependency\"" }

        project.build("build", options = options) {
            assertSuccessful()
        }

        buildGradle.modify { it.replace(commentedOutDependency, dependency) }
        project.build("build", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(project.projectDir.allKotlinFiles()))
        }
    }

    @Test
    fun testRemoveDependencyNonIC() {
        doTestRemoveDependency(defaultBuildOptions().copy())
    }

    @Test
    fun testRemoveDependencyIC() {
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