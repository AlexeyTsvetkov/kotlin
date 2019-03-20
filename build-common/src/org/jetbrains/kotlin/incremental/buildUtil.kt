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

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.build.isModuleMappingFile
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.synthetic.SAM_LOOKUP_NAME
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import java.io.File
import java.util.*
import kotlin.collections.HashSet

const val DELETE_MODULE_FILE_PROPERTY = "kotlin.delete.module.file.after.build"

fun makeModuleFile(
        name: String,
        isTest: Boolean,
        outputDir: File,
        sourcesToCompile: Iterable<File>,
        commonSources: Iterable<File>,
        javaSourceRoots: Iterable<JvmSourceRoot>,
        classpath: Iterable<File>,
        friendDirs: Iterable<File>
): File {
    val builder = KotlinModuleXmlBuilder()
    builder.addModule(
            name,
            outputDir.absolutePath,
            // important to transform file to absolute paths,
            // otherwise compiler will use module file's parent as base path (a temporary file; see below)
            // (see org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.getAbsolutePaths)
            sourcesToCompile.map { it.absoluteFile },
            javaSourceRoots,
            classpath,
            commonSources.map { it.absoluteFile },
            null,
            "java-production",
            isTest,
            // this excludes the output directories from the class path, to be removed for true incremental compilation
            setOf(outputDir),
            friendDirs
    )

    val scriptFile = File.createTempFile("kjps", StringUtil.sanitizeJavaIdentifier(name) + ".script.xml")
    FileUtil.writeToFile(scriptFile, builder.asText().toString())
    return scriptFile
}

fun makeCompileServices(
        incrementalCaches: Map<TargetId, IncrementalCache>,
        lookupTracker: LookupTracker,
        compilationCanceledStatus: CompilationCanceledStatus?
): Services =
    with(Services.Builder()) {
        register(LookupTracker::class.java, lookupTracker)
        register(IncrementalCompilationComponents::class.java, IncrementalCompilationComponentsImpl(incrementalCaches))
        compilationCanceledStatus?.let {
            register(CompilationCanceledStatus::class.java, it)
        }
        build()
    }

fun updateIncrementalCache(
    generatedFiles: Iterable<GeneratedFile>,
    cache: IncrementalJvmCache,
    changesCollector: ChangesCollector,
    javaChangesTracker: JavaClassesTrackerImpl?
) {
    for (generatedFile in generatedFiles) {
        when {
            generatedFile is GeneratedJvmClass -> cache.saveFileToCache(generatedFile, changesCollector)
            generatedFile.outputFile.isModuleMappingFile() -> cache.saveModuleMappingToCache(generatedFile.sourceFiles, generatedFile.outputFile)
        }
    }

    javaChangesTracker?.javaClassesUpdates?.forEach {
        (source, serializedJavaClass) ->
        cache.saveJavaClassProto(source, serializedJavaClass, changesCollector)
    }

    cache.clearCacheForRemovedClasses(changesCollector)
}

fun LookupStorage.update(
        lookupTracker: LookupTracker,
        filesToCompile: Iterable<File>,
        removedFiles: Iterable<File>
) {
    if (lookupTracker !is LookupTrackerImpl) throw AssertionError("Lookup tracker is expected to be LookupTrackerImpl, got ${lookupTracker::class.java}")

    removeLookupsFrom(filesToCompile.asSequence() + removedFiles.asSequence())

    addAll(lookupTracker.lookups.entrySet(), lookupTracker.pathInterner.values)
}

data class DirtyData(
    val dirtyLookupSymbols: Collection<LookupSymbol> = emptyList(),
    val dirtyClassesFqNames: Collection<FqName> = emptyList()
)

fun ChangesCollector.getDirtyData(
    caches: Iterable<IncrementalCacheCommon>,
    reporter: ICReporter
): DirtyData =
    getDirtyData(changes(), SubclassesProviderImpl(caches), reporter)

fun getDirtyData(
    changes: Collection<ChangeInfo>,
    subclassesProvider: SubclassesProvider,
    reporter: ICReporter
): DirtyData {
    val dirtyLookupSymbols = HashSet<LookupSymbol>()
    val dirtyClassesFqNames = HashSet<FqName>()

    for (change in changes) {
        reporter.reportVerbose { "Process $change" }
        val subclassesFqNames = subclassesProvider.subclasses(change.fqName)

        if (change.isSignatureChanged) {
            // recompile modified class usages
            dirtyLookupSymbols.add(change.fqName.toLookupSymbol())

            if (change.areSubclassesAffected) {
                // recompile declarations of subclasses
                dirtyClassesFqNames.addAll(subclassesFqNames)
                // recompile usages of subclasses
                subclassesFqNames.mapTo(dirtyLookupSymbols) { it.toLookupSymbol() }
            }
        }

        if (change.changedMembers.isNotEmpty()) {
            // recompile declarations of subclasses, because changed member might break override
            dirtyClassesFqNames.addAll(subclassesFqNames)

            // recompile usages of all members from all classes
            val allChangedMembers = change.changedMembers + SAM_LOOKUP_NAME.asString()
            for (classFqName in listOf(change.fqName) + subclassesFqNames) {
                allChangedMembers.mapTo(dirtyLookupSymbols) { memberName ->
                    LookupSymbol(scope = classFqName.asString(), name = memberName)
                }
            }
        }
    }

    return DirtyData(dirtyLookupSymbols, dirtyClassesFqNames)
}

private fun FqName.toLookupSymbol(): LookupSymbol =
    LookupSymbol(name = shortName().identifier, scope = parent().asString())

fun mapLookupSymbolsToFiles(
    lookupStorage: LookupStorage,
    lookupSymbols: Iterable<LookupSymbol>,
    reporter: ICReporter,
    excludes: Set<File> = emptySet()
): Set<File> {
    val dirtyFiles = HashSet<File>()

    for (lookup in lookupSymbols) {
        val affectedFiles = lookupStorage.get(lookup).map(::File).filter { it !in excludes }
        reporter.reportMarkDirtyMember(affectedFiles, scope = lookup.scope, name = lookup.name)
        dirtyFiles.addAll(affectedFiles)
    }

    return dirtyFiles
}

fun mapClassesFqNamesToFiles(
    caches: Iterable<IncrementalCacheCommon>,
    classesFqNames: Iterable<FqName>,
    reporter: ICReporter,
    excludes: Set<File> = emptySet()
): Set<File> {
    val fqNameToAffectedFiles = HashMap<FqName, MutableSet<File>>()

    for (cache in caches) {
        for (classFqName in classesFqNames) {
            val srcFile = cache.getSourceFileIfClass(classFqName)
            if (srcFile == null || srcFile in excludes || srcFile.isJavaFile()) continue

            fqNameToAffectedFiles.getOrPut(classFqName) { HashSet() }.add(srcFile)
        }
    }

    for ((classFqName, affectedFiles) in fqNameToAffectedFiles) {
        reporter.reportMarkDirtyClass(affectedFiles, classFqName.asString())
    }

    return fqNameToAffectedFiles.values.flattenTo(HashSet())
}


