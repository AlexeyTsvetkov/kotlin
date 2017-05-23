/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.storage.*
import java.io.DataInput
import java.io.DataOutput
import java.io.File

open class JsIncrementalCache(
        private val cachesDir: File
) : BasicMapsOwner() {
    companion object {
        private val TRANSLATION_RESULT_MAP = "translation-result"
    }

    protected val String.storageFile: File
        get() = File(cachesDir, this + "." + CACHE_EXTENSION)

    private val translationResults = registerMap(TranslationResultMap(TRANSLATION_RESULT_MAP.storageFile))

    fun registerDirtySourceFiles(dirtySourceFiles: List<File>) {
        dirtySourceFiles.forEach { translationResults.remove(it) }
    }
}

@Suppress("ArrayInDataClass")
private data class TranslationResultValue(val metadata: ByteArray, val binaryAst: ByteArray)
private object TranslationResultValueExternalizer : DataExternalizer<TranslationResultValue> {
    override fun save(output: DataOutput, value: TranslationResultValue) {
        output.writeInt(value.metadata.size)
        output.write(value.metadata)

        output.writeInt(value.binaryAst.size)
        output.write(value.binaryAst)
    }

    override fun read(input: DataInput): TranslationResultValue {
        val metadataSize = input.readInt()
        val metadata = ByteArray(metadataSize)
        input.readFully(metadata)

        val binaryAstSize = input.readInt()
        val binaryAst = ByteArray(binaryAstSize)
        input.readFully(binaryAst)

        return TranslationResultValue(metadata = metadata, binaryAst = binaryAst)
    }
}

private class TranslationResultMap(storageFile: File) : BasicStringMap<TranslationResultValue>(storageFile, TranslationResultValueExternalizer) {
    override fun dumpValue(value: TranslationResultValue): String =
            "Metadata: ${value.metadata.md5String()}, Binary AST: ${value.binaryAst.md5String()}"

    fun put(file: File, metadata: ByteArray, binaryAst: ByteArray) {
        storage[file.canonicalPath] = TranslationResultValue(metadata = metadata, binaryAst = binaryAst)
    }

    fun values(): Collection<TranslationResultValue> =
            storage.keys.map { storage[it]!! }

    fun remove(file: File) {
        storage.remove(file.canonicalPath)
    }
}