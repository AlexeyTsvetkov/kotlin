package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.junit.Test

class IncrementalCompilationMultiProjectIT : BaseGradleIT() {
    companion object {
        private val GRADLE_VERSION = "2.10"
    }

    override fun defaultBuildOptions(): BuildOptions =
            super.defaultBuildOptions().copy(withDaemon = true, incremental = true)

    @Test
    fun testAddNewMethodToLib() {
        val project = Project("incrementalMultiproject", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
        }

        val aKt = project.projectDir.getFileByName("A.kt")
        aKt.writeText("""
package bar

open class A {
    fun a() {}
    fun newA() {}
}
""")

        project.build("build", "-Dorg.gradle.debug=true") {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("A.kt", "B.kt", "barUseA.kt", "AA.kt", "BB.kt", "fooUseA.kt")
            assertCompiledKotlinSources(project.relativize(affectedSources), weakTesting = false)
        }
    }
}