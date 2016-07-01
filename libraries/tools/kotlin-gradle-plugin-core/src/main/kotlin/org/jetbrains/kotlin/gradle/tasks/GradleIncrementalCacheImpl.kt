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

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.lang.Language
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.com.intellij.util.io.BooleanDataDescriptor
import org.jetbrains.kotlin.com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.config.CompilerConfiguration
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
    private val javaSymbolsMap = registerMap(JavaSymbols(CLASSPATH.storageFile))

    fun compareClasspath(currentClasspath: Set<File>): FileDifference {
        val added = currentClasspath.asSequence().filter { it !in classpathMap }
        val removed = classpathMap.values.asSequence().filter { it !in currentClasspath }
        return FileDifference(added, removed)
    }

    fun updateClasspath(currentClasspath: Set<File>) {
        classpathMap.clean()
        currentClasspath.forEach { classpathMap.add(it) }
    }

    fun processJavaFiles(newJavaFiles: Iterable<File>, removedJavaFiles: Iterable<File>): Collection<LookupSymbol> {
        val affectedSymbols = HashSet<LookupSymbol>()

        val rootDisposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration()
        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = environment.project
        val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl


        modifiedJavaFiles.flatMap {
            val javaFile = psiFileFactory.createFileFromText(it.nameWithoutExtension, Language.findLanguageByID("JAVA")!!, it.readText())
            if (javaFile is PsiJavaFile)
                javaFile.classes.flatMap { it.findLookupSymbols() }
            else listOf()
        }
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

    operator fun contains(file: File): Boolean =
            storage.contains(file.canonicalPath)

    override fun dumpValue(value: Boolean) = ""
}

private class ClasspathSet(storageFile: File) : PersistentFileSet(storageFile)

private object LookupSymbolExternalizer : DataExternalizer<LookupSymbol> {
    override fun read(input: DataInput): LookupSymbol {
        val name = input.readUTF()
        val scope = input.readUTF()
        return LookupSymbol(name, scope)
    }

    override fun save(output: DataOutput, value: LookupSymbol) {
        output.writeUTF(value.name)
        output.writeUTF(value.scope)
    }
}

private object LookupSymbolsCollectionExternalizer : CollectionExternalizer<LookupSymbol>(LookupSymbolExternalizer, { HashSet() })

private class JavaSymbols(storageFile: File) : BasicStringMap<Collection<LookupSymbol>>(storageFile, LookupSymbolsCollectionExternalizer) {
    operator fun set(javaFile: File, symbols: Collection<LookupSymbol>) {
        storage[javaFile.canonicalPath] = symbols
    }

    operator fun get(javaFile: File): Collection<LookupSymbol> =
        storage[javaFile.canonicalPath] ?: emptySet()

    fun remove(javaFile: File) {
        storage.remove(javaFile.canonicalPath)
    }

    override fun dumpValue(value: Collection<LookupSymbol>): String =
            "[" + value.map { it.scope + "." + it.name }.sorted().joinToString() + "]"
}