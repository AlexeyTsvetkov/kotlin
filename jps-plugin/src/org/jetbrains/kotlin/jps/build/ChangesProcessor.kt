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

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.FSOperations
import org.jetbrains.jps.incremental.fs.CompilationRound
import org.jetbrains.kotlin.jps.incremental.ChangesInfo
import org.jetbrains.kotlin.jps.incremental.IncrementalCacheImpl
import java.io.File

interface ChangesProcessor {
    fun processChanges(changesInfo: ChangesInfo)

    companion object {
        val DO_NOTHING = object : ChangesProcessor {
            override fun processChanges(changesInfo: ChangesInfo) {}
        }
    }
}

internal class ChangesProcessorImpl(
        val context: CompileContext,
        val chunk: ModuleChunk,
        val allCompiledFiles: MutableSet<File>,
        val caches: List<IncrementalCacheImpl>
): ChangesProcessor {
    override fun processChanges(changesInfo: ChangesInfo) {
        changesInfo.doProcessChanges()
    }

    private fun ChangesInfo.doProcessChanges() {
        fun isKotlin(file: File) = KotlinSourceFileCollector.isKotlinSourceFile(file)
        fun isNotCompiled(file: File) = file !in allCompiledFiles

        when {
            inlineAdded -> {
                allCompiledFiles.clear()
                FSOperations.markDirtyRecursively(context, CompilationRound.NEXT, chunk, ::isKotlin)
                return
            }
            constantsChanged -> {
                FSOperations.markDirtyRecursively(context, CompilationRound.NEXT, chunk, ::isNotCompiled)
                return
            }
            protoChanged -> {
                FSOperations.markDirty(context, CompilationRound.NEXT, chunk, { isKotlin(it) && isNotCompiled(it) })
            }
        }

        if (inlineChanged) {
            recompileInlined()
        }
    }

    private fun recompileInlined() {
        for (cache in caches) {
            val filesToReinline = cache.getFilesToReinline()

            filesToReinline.forEach {
                FSOperations.markDirty(context, CompilationRound.NEXT, it)
            }
        }
    }
}
