package org.jetbrains.kotlin.bytecode

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil.createTempDirectory
import org.jetbrains.kotlin.incremental.testingUtils.classFileToString
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.properties.Delegates

class AnnotationsRemoverTest {
    private var workingDir: File by Delegates.notNull()

    @Before
    fun setUp() {
        workingDir = createTempDirectory(AnnotationsRemoverTest::class.java.simpleName, null)
    }

    @After
    fun tearDown() {
        workingDir.deleteRecursively()
    }

    @Test
    fun testRemoveAnnotations() {
        // initial build
        val sourceDir = File(workingDir, "src").apply { mkdirs() }
        File(sourceDir, "annotations.kt").apply {
            writeText("""
                package foo

                annotation class Ann1
                annotation class Ann2
                annotation class Ann3
            """.trimIndent())
        }
        val aSource = """
            import foo.*

            @Ann1
            class A {
                @Ann2
                val i = 10

                @Ann3
                fun m() {}
            }
        """.trimIndent()
        val aKt = File(sourceDir, "A.kt").apply {
            writeText(aSource)
        }
        val outDir = File(workingDir, "out").apply { mkdirs() }
        compileAll(sourceDir, outDir)
        val aClass = File(outDir, "A.class")
        assert(aClass.exists()) { "$aClass does not exist" }

        // remove annotations
        val transformedOut = File(workingDir, "transformed").apply { mkdirs() }
        val aTransformedClass = File(transformedOut, "A.class")
        val remover = AnnotationsRemover(setOf("foo/Ann1", "foo/Ann2", "foo/Ann3"))
        remover.transformClassFile(aClass, aTransformedClass)
        val transformedClassBytecode = classBytecodeToString(aTransformedClass)

        // compile source without annotations
        val aSourceWithoutAnns = "@Ann\\d+".toRegex().replace(aSource, "")
        aKt.writeText(aSourceWithoutAnns)
        compileAll(sourceDir, outDir)
        val classWithoutAnnsBytecode = classBytecodeToString(aClass)

        assertEquals(classWithoutAnnsBytecode, transformedClassBytecode)
    }

    private fun compileAll(inputDir: File, outputDir: File) {
        val ktFiles = inputDir.walk()
                .filter { it.isFile && it.extension.toLowerCase() == "kt" }
                .map { it.absolutePath }
                .toList().toTypedArray()

        val exitCode = K2JVMCompiler().exec(System.err, *ktFiles, "-d", outputDir.absolutePath)
        assertEquals(ExitCode.OK, exitCode)
    }

    private fun classBytecodeToString(classFile: File): String {
        val out = StringWriter()
        val traceVisitor = TraceClassVisitor(PrintWriter(out))
        ClassReader(classFile.readBytes()).accept(traceVisitor, 0)
        return out.toString()
    }
}