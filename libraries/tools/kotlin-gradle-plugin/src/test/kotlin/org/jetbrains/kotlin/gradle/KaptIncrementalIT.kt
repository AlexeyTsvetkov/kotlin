package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.findFileByName
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test

class KaptIncrementalIT: BaseGradleIT() {

    companion object {
        private const val GRADLE_VERSION = "2.10"
        private val EXAMPLE_ANNOTATION_REGEX = "@field:example.ExampleAnnotation".toRegex()
    }

    override fun defaultBuildOptions(): BuildOptions =
            super.defaultBuildOptions().copy(withDaemon = true, incremental = true)

    @Test
    fun testBasic() {
        getProject().build("build") {
            assertSuccessful()
            checkGenerated("A", "funA", "valA", "B", "funB", "valB", "funUtil", "valUtil")
        }
    }

    @Test
    fun testRemoveSourceContainingClass() {
        val project = getProject()
        project.build("build") {
            assertSuccessful()
        }

        val bKt = project.projectDir.getFileByName("B.kt")
        bKt.modify { it.replace(EXAMPLE_ANNOTATION_REGEX, "") }

        project.build("build") {
            assertSuccessful()
            //assertCompiledKotlinSources(emptySet(), weakTesting = false)
            checkGenerated("A", "funA", "valA", "funUtil", "valUtil")
            checkNotGenerated("B", "funB", "valB")
        }
    }

    private fun getProject() = Project("kaptIncrementalCompilationProject", GRADLE_VERSION)

    private fun CompiledProject.checkGenerated(vararg annotatedElementNames: String) {
        getGeneratedFileNames(*annotatedElementNames).forEach {
            val file = project.projectDir.getFileByName(it)
            assert(file.isFile) { "$file must exist" }
        }
    }

    private fun CompiledProject.checkNotGenerated(vararg annotatedElementNames: String) {
        getGeneratedFileNames(*annotatedElementNames).forEach {
            val file = project.projectDir.findFileByName(it)
            assert(file == null) { "$file must not exist" }
        }
    }

    private fun getGeneratedFileNames(vararg annotatedElementNames: String): Iterable<String> {
        val names = annotatedElementNames.map { it.capitalize() + "Generated" }
        return names.map { it + ".java" }
    }
}