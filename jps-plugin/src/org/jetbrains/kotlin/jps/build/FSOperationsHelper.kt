/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.FSOperations
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.fs.CompilationRound
import java.io.File

class FSOperationsHelper(
        private val compileContext: CompileContext,
        private val chunk: ModuleChunk,
        private val log: Logger
) {
    internal var hasMarkedDirty = false
        private set

    private val buildLogger = compileContext.testingContext?.buildLogger

    fun markChunk(recursively: Boolean, kotlinOnly: Boolean, excludeFiles: Set<File> = setOf()) {
        fun shouldMark(file: File): Boolean {
            if (kotlinOnly && !KotlinSourceFileCollector.isKotlinSourceFile(file)) return false

            if (file in excludeFiles) return false

            hasMarkedDirty = true
            return true
        }

        if (recursively) {
            FSOperations.markDirtyRecursively(compileContext, CompilationRound.NEXT, chunk, ::shouldMark)
        }
        else {
            FSOperations.markDirty(compileContext, CompilationRound.NEXT, chunk, ::shouldMark)
        }
    }

    fun markFiles(files: Iterable<File>, excludeFiles: Set<File> = setOf()) {
        val filesToMark = files.filterTo(HashSet()) {
            it !in excludeFiles && it.exists()
        }

        removeFilesFromCompiledTargets(filesToMark)

        if (filesToMark.isEmpty()) return

        for (fileToMark in filesToMark) {
            FSOperations.markDirty(compileContext, CompilationRound.NEXT, fileToMark)
        }

        log.debug("Mark dirty: $filesToMark")
        buildLogger?.markedAsDirty(filesToMark)
        hasMarkedDirty = true
    }

    // remove files from chunks preceding current chunk in a build
    private fun removeFilesFromCompiledTargets(filesToMark: HashSet<File>) {
        val targetToDirtyFiles = groupFilesByTargets(filesToMark)

        chunk.targets.forEach { targetToDirtyFiles.remove(it) }
        if (targetToDirtyFiles.isEmpty()) {
            // all dirty targets are from current chunk
            return
        }

        val buildTargetIndex = compileContext.projectDescriptor.buildTargetIndex
        val sortedTargetChunks = buildTargetIndex.getSortedTargetChunks(compileContext)

        for (targetChunk in sortedTargetChunks) {
            if (targetChunk.targets == chunk.targets) {
                // found current chunk
                return
            }

            for (compiledTarget in targetChunk.targets) {
                val filesFromCompiledTarget = targetToDirtyFiles[compiledTarget] ?: continue
                filesToMark.removeAll(filesFromCompiledTarget)
            }
        }
    }

    private fun groupFilesByTargets(filesToMark: MutableSet<File>): MutableMap<ModuleBuildTarget, out Collection<File>> {
        val buildRootIndex = compileContext.projectDescriptor.buildRootIndex
        val targetToDirtyFiles = HashMap<ModuleBuildTarget, MutableSet<File>>()
        for (dirtyFile in filesToMark) {
            val javaRoot = buildRootIndex.findJavaRootDescriptor(compileContext, dirtyFile) ?: continue
            val dirtyFilesForTarget = targetToDirtyFiles.getOrPut(javaRoot.target) { HashSet() }
            dirtyFilesForTarget.add(dirtyFile)
        }
        return targetToDirtyFiles
    }
}
