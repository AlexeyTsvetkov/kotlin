package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.junit.Test

class SimpleKotlinGradleIT : BaseGradleIT() {

    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

    @Test
    fun testSimpleCompile() {
        val project = Project("simpleProject", GRADLE_VERSION)

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertReportExists("build/reports/tests/classes/demo.TestSource.html")
            assertContains(":compileKotlin", ":compileTestKotlin", ":compileDeployKotlin")
        }

        project.build("compileDeployKotlin", "build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE", ":compileTestKotlin UP-TO-DATE", ":compileDeployKotlin UP-TO-DATE", ":compileJava UP-TO-DATE")
        }
    }

    @Test
    fun testSuppressWarningsAndVersionInVerboseMode() {
        val project = Project("suppressWarningsAndVersion", GRADLE_VERSION)

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin", "i: Kotlin Compiler version", "v: Using Kotlin home directory")
            assertNotContains("w:")
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
            assertNotContains("w:")
        }
    }

    @Test
    fun testSuppressWarningsAndVersionInNonVerboseMode() {
        val project = Project("suppressWarningsAndVersion", GRADLE_VERSION, minLogLevel = LogLevel.INFO)

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin", "i: Kotlin Compiler version")
            assertNotContains("w:", "v:")
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
            assertNotContains("w:", "v:")
        }
    }

    @Test
    fun testKotlinCustomDirectory() {
        Project("customSrcDir", GRADLE_VERSION).build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testKotlinCustomModuleName() {
        Project("moduleNameCustom", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertContains("args.moduleName = myTestName")
        }
    }

    @Test
    fun testKotlinDefaultModuleName() {
        Project("moduleNameDefault", GRADLE_VERSION).build("build") {
            assertSuccessful()
            assertContains("args.moduleName = moduleNameDefault-compileKotlin")
        }
    }

    @Test
    fun testAdvancedOptions() {
        Project("advancedOptions", GRADLE_VERSION).build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testKotlinExtraJavaSrc() {
        Project("additionalJavaSrc", GRADLE_VERSION).build("build") {
            assertSuccessful()
        }
    }

    @Test
    fun testLanguageVersion() {
        Project("languageVersion", GRADLE_VERSION).build("build") {
            assertFailed()
            assertContains("This type is sealed")
        }
    }

    @Test
    fun testGradleSubplugin() {
        val project = Project("kotlinGradleSubplugin", GRADLE_VERSION)

        project.build("compileKotlin", "build") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertContains("Project component registration: exampleValue")
            assertContains(":compileKotlin")
        }

        project.build("compileKotlin", "build") {
            assertSuccessful()
            assertContains("ExampleSubplugin loaded")
            assertNotContains("Project component registration: exampleValue")
            assertContains(":compileKotlin UP-TO-DATE")
        }
    }
}