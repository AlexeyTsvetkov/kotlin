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

package org.jetbrains.kotlin.gradle.tasks

import com.intellij.util.io.BooleanDataDescriptor
import com.intellij.util.io.DataExternalizer
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.incremental.CompilationResult
import org.jetbrains.kotlin.incremental.IncrementalCacheImpl
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.dumpCollection
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.CollectionExternalizer
import org.jetbrains.kotlin.incremental.storage.PathStringDescriptor
import org.jetbrains.kotlin.incremental.storage.StringCollectionExternalizer
import org.jetbrains.kotlin.modules.TargetId
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import java.util.*

class GradleIncrementalCacheImpl(targetDataRoot: File, targetOutputDir: File?, target: TargetId) : IncrementalCacheImpl<TargetId>(targetDataRoot, targetOutputDir, target) {

    companion object {
        private val SOURCES_TO_CLASSFILES = "sources-to-classfiles"
        private val CLASSPATH = "classpath"
        private val JAVA_SYMBOLS = "java-symbols"
    }

    private val loggerInstance = Logging.getLogger(this.javaClass)
    fun getLogger() = loggerInstance

    private val sourceToClassfilesMap = registerMap(SourceToClassfilesMap(SOURCES_TO_CLASSFILES.storageFile))
    private val classpathMap = registerMap(ClasspathSet(CLASSPATH.storageFile))
    private val javaSymbolsMap = registerMap(JavaFileSymbolsMap(JAVA_SYMBOLS.storageFile))

    fun compareClasspath(currentClasspath: Set<File>): FileDifference {
        val added = currentClasspath.asSequence().filter { it !in classpathMap }
        val removed = classpathMap.values.asSequence().filter { it !in currentClasspath }
        return FileDifference(added, removed)
    }

    fun updateClasspath(currentClasspath: Set<File>) {
        classpathMap.clean()
        currentClasspath.forEach { classpathMap.add(it) }
    }


    fun removeClassfilesBySources(sources: Iterable<File>): Unit =
            sources.forEach { sourceToClassfilesMap.remove(it) }

    override fun saveFileToCache(generatedClass: GeneratedJvmClass<TargetId>): CompilationResult {
        generatedClass.sourceFiles.forEach { sourceToClassfilesMap.add(it, generatedClass.outputFile) }
        return super.saveFileToCache(generatedClass)
    }

    inner class SourceToClassfilesMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer) {
        fun add(sourceFile: File, classFile: File) {
            storage.append(sourceFile.absolutePath, classFile.absolutePath)
        }

        operator fun get(sourceFile: File): Collection<File> =
                storage[sourceFile.absolutePath].orEmpty().map { File(it) }

        override fun dumpValue(value: Collection<String>) = value.dumpCollection()

        fun remove(file: File) {
            // TODO: do it in the code that uses cache, since cache should not generally delete anything outside of it!
            // but for a moment it is an easiest solution to implement
            get(file).forEach {
                getLogger().debug("[KOTLIN] Deleting $it on clearing cache for $file")
                it.delete()
            }
            storage.remove(file.absolutePath)
        }
    }
}

class FileDifference(val added: Sequence<File>, val removed: Sequence<File>) {
    fun isNotEmpty(): Boolean =
            added.any() || removed.any()
}

private abstract class PersistentFileSet(storageFile: File) : BasicStringMap<Boolean>(storageFile, BooleanDataDescriptor.INSTANCE) {
    val values: Iterable<File>
        get() = storage.keys.map { File(it) }

    fun add(file: File) {
        storage[file.canonicalPath] = true
    }

    fun remove(file: File) {
        storage.remove(file.canonicalPath)
    }

    operator fun contains(file: File): Boolean =
            storage.contains(file.canonicalPath)

    override fun dumpValue(value: Boolean) = ""
}

private class ClasspathSet(storageFile: File) : PersistentFileSet(storageFile)

private class JavaFileSymbolsMap(storageFile: File) : BasicStringMap<Collection<LookupSymbol>>(storageFile, LookupSymbolCollectionExternalizer) {
    operator fun get(file: File): Collection<LookupSymbol> =
            storage[file.canonicalPath] ?: emptySet()

    operator fun set(file: File, symbols: Collection<LookupSymbol>) {
        storage[file.canonicalPath] = symbols
    }

    operator fun contains(file: File): Boolean =
            storage.contains(file.canonicalPath)

    fun remove(file: File) {
        storage.remove(file.canonicalPath)
    }

    override fun dumpValue(value: Collection<LookupSymbol>): String =
            value.sortedWith(LookupSymbolComparator).joinToString()
}

private object LookupSymbolComparator : Comparator<LookupSymbol> {
    override fun compare(o1: LookupSymbol, o2: LookupSymbol): Int {
        val compareName = o1.name.compareTo(o2.name)
        if (compareName != 0) return compareName

        return o1.scope.compareTo(o2.scope)
    }

}

private object LookupSymbolExternalizer : DataExternalizer<LookupSymbol> {
    override fun save(output: DataOutput, value: LookupSymbol) {
        output.writeUTF(value.name)
        output.writeUTF(value.scope)
    }

    override fun read(input: DataInput): LookupSymbol {
        val name = input.readUTF()
        val scope = input.readUTF()
        return LookupSymbol(name, scope)
    }
}

private object LookupSymbolCollectionExternalizer : CollectionExternalizer<LookupSymbol>(LookupSymbolExternalizer, { HashSet() })