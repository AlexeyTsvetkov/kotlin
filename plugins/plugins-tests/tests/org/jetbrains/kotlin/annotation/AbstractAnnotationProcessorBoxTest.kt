/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.annotation

import com.google.protobuf.ExtensionRegistry
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.serialization.DebugProtoBuf
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.serialization.jvm.DebugJvmProtoBuf
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringWriter

private fun readKotlinHeader(bytes: ByteArray): KotlinClassHeader {
    var header: KotlinClassHeader? = null

    FileBasedKotlinClass.create(bytes) { className, classHeader, innerClasses ->
        header = classHeader
        null
    }

    if (header == null) throw AssertionError("Could not read kotlin header from byte array")

    return header!!
}

private fun classFileToString(classHeader: KotlinClassHeader): String {
    val out = StringWriter()
    val annotationDataEncoded = classHeader.data

    if (annotationDataEncoded != null) {
        ByteArrayInputStream(BitEncoding.decodeBytes(annotationDataEncoded)).use { input ->

            out.write("\n------ string table types proto -----\n${DebugJvmProtoBuf.StringTableTypes.parseDelimitedFrom(input)}")

            when (classHeader.kind) {
                KotlinClassHeader.Kind.FILE_FACADE -> {
                    val parseFrom = DebugProtoBuf.Package.parseFrom(input, getExtensionRegistry())
                    out.write("\n------ file facade proto -----\n$parseFrom")
                }
                KotlinClassHeader.Kind.CLASS -> {
                    val parseFrom = DebugProtoBuf.Class.parseFrom(input, getExtensionRegistry())
                    out.write("\n------ class proto -----\n$parseFrom")
                }
                KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                    val parseFrom = DebugProtoBuf.Package.parseFrom(input, getExtensionRegistry())
                    out.write("\n------ multi-file part proto -----\n$parseFrom")
                }
                else -> throw IllegalStateException()
            }
        }
    }

    return out.toString()
}

private fun getExtensionRegistry(): ExtensionRegistry {
    val registry = ExtensionRegistry.newInstance()!!
    DebugJvmProtoBuf.registerAllExtensions(registry)
    return registry
}

abstract class AbstractAnnotationProcessorBoxTest : CodegenTestCase() {
    override fun doTest(path: String) {
        val testName = getTestName(true)
        val ktFiles = File(path).listFiles { file -> file.isFile && file.extension.toLowerCase() == "kt" }
        val testFiles = ktFiles.map { TestFile(it.name, it.readText()) }
        val supportInheritedAnnotations = testName.startsWith("inherited")

        val collectorExtension = createTestEnvironment(supportInheritedAnnotations)
        loadMultiFiles(testFiles)
        val generatedFiles = CodegenTestUtil.generateFiles(myEnvironment, myFiles)
        val classFiles = generatedFiles.getClassFiles()
        for (file in classFiles) {
            val bytes = file.asByteArray()
            val header = readKotlinHeader(bytes)

            val string = classFileToString(header)
            val x = 0
        }

        val actualAnnotations = KotlinTestUtils.replaceHashWithStar(collectorExtension.stringWriter.toString())
        val expectedAnnotationsFile = File(path + "annotations.txt")

        KotlinTestUtils.assertEqualsToFile(expectedAnnotationsFile, actualAnnotations)
    }

    override fun codegenTestBasePath(): String {
        return "plugins/annotation-collector/testData/codegen/"
    }

    private fun createTestEnvironment(supportInheritedAnnotations: Boolean): AnnotationCollectorExtensionForTests {
        val configuration = KotlinTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = environment.project

        val collectorExtension = AnnotationCollectorExtensionForTests(supportInheritedAnnotations)
        ClassBuilderInterceptorExtension.registerExtension(project, collectorExtension)

        myEnvironment = environment

        return collectorExtension
    }

    private class AnnotationCollectorExtensionForTests(
            supportInheritedAnnotations: Boolean
    ) : AnnotationCollectorExtensionBase(supportInheritedAnnotations) {
        val stringWriter = StringWriter()

        override fun getWriter(diagnostic: DiagnosticSink) = stringWriter
        override fun closeWriter() {}

        override val annotationFilterList = listOf<String>()
    }
}
