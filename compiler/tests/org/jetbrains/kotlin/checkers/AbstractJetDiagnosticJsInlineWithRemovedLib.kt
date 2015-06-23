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

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils.convertArgumentsToStringList
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.js.descriptorUtils.ModuleDescriptor
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File

public abstract class AbstractJetDiagnosticJsInlineWithRemovedLib : AbstractJetDiagnosticsTestWithJsStdLibAndBackendCompilation() {

    private var libMetaJs: File? = null

    override fun setUp() {
        super.setUp()
        libMetaJs = null
    }

    override fun doTest(filePath: String) {
        if (!filePath.endsWith(KT_EXT)) throw AssertionError("Test file does not have Kotlin extension ($KT_EXT)")

        val libKtFile = File(filePath)
        val testName = filePath.removeSuffix(LIB_KT)
        val testFile = File(testName + KT_EXT)
        val outputDir = createOutputDirectoryForTest(testName, testFile)
        val libJs = File(outputDir, LIB_JS)
        libMetaJs = File(outputDir, LIB_META_JS)

        libKtFile.compileTo(libJs)
        assert(libJs.isFile()) { "$libJs was expected to be created by compiler" }
        FileUtil.delete(libJs)

        super.doTest(testFile.getPath())
    }

    private fun File.compileTo(outputJs: File) {
        val settings = K2JSCompilerArguments()
        settings.outputFile = outputJs.getPath()
        settings.main = K2JsArgumentConstants.NO_CALL
        settings.metaInfo = true
        val arguments = convertArgumentsToStringList(settings) + getPath()
        K2JSCompiler.main(*arguments.toTypedArray())
    }

    override fun commonDependencies(storageManager: StorageManager): List<ModuleDescriptorImpl> {
        assert(libMetaJs?.isFile() == true) { ".lib.meta.js was not compiled" }
        val libMetadata = KotlinJavascriptMetadataUtils.loadMetadata(libMetaJs!!)
        val libModule = ModuleDescriptor(libMetadata.first(), storageManager)
        val dependencies = super.commonDependencies(storageManager)
        setUpDependeciesAndSealModule(libModule, dependencies)
        return dependencies + listOf(libModule)
    }
}

private val KT_EXT = ".kt"
private val LIB_KT = ".lib.kt"
private val OUT_DIR_NAME = "out"
private val LIB_META_JS = "lib.meta.js"
private val LIB_JS = "lib.js"

private fun createOutputDirectoryForTest(testName: String, testKt: File): File {
    val normalizedName = testName.replace("\\W+".toRegex(), "")
    val outDir = File(testKt.parent, OUT_DIR_NAME)
    val outputDir = File(outDir, normalizedName)

    if (outputDir.isDirectory()) {
        FileUtil.delete(outputDir)
    }

    FileUtil.createDirectory(outputDir)
    return outputDir
}
