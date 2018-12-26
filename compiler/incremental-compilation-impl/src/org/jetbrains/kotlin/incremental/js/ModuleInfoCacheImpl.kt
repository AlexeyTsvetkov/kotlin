/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.js

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.PathStringDescriptor
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMap
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapGroup
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSegment
import java.io.DataInput
import java.io.DataOutput
import java.io.File

class ModuleInfoCacheImpl(cachesDir: File) : BasicMapsOwner(cachesDir), ModuleInfoCache {
    companion object {
        private const val MODULES_INFO = "m-info"
        private const val VERSION = SpecialFunction.version
        private const val VERSION_FILE_NAME = "m-info-version.txt"

        private fun readVersion(file: File): Int? {
            if (!file.exists()) return null

            return try {
                file.readText().toInt()
            } catch (e: Throwable) {
                null
            }
        }

        private fun saveCurrentVersion(file: File) {
            file.writeText(VERSION.toString())
        }
    }

    private val moduleInfoMap = registerMap(ModuleInfoMap(MODULES_INFO.storageFile))
    private val cacheVersionFile: File
        get() = File(cachesDir, VERSION_FILE_NAME)

    init {
        if (readVersion(cacheVersionFile) != VERSION) {
            clean()
        }
    }

    override fun get(file: File): Collection<ModuleInfoValue>? =
        moduleInfoMap.get(file)

    override fun set(file: File, moduleInfoValue: Collection<ModuleInfoValue>) {
        if (!file.extension.equals("jar", ignoreCase = true)) {
            return
        }

        moduleInfoMap.set(file, moduleInfoValue)
    }

    fun remove(file: File) {
        moduleInfoMap.remove(file)
    }

    override fun flush() {
        super.flush(memoryCachesOnly = false)
        saveCurrentVersion(cacheVersionFile)
    }

    private class ModuleInfoMap(
        storageFile: File
    ) : BasicStringMap<Collection<ModuleInfoValue>>(
        storageFile,
        PathStringDescriptor,
        ModuleInfoCollectionExternalizer
    ) {
        fun get(file: File): Collection<ModuleInfoValue>? =
            storage[file.canonicalPath]

        fun set(file: File, moduleInfoValue: Collection<ModuleInfoValue>) {
            storage[file.canonicalPath] = moduleInfoValue
        }

        fun remove(file: File) {
            storage.remove(file.canonicalPath)
        }

        override fun dumpValue(value: Collection<ModuleInfoValue>): String {
            return ""
        }
    }

    private object ModuleInfoCollectionExternalizer :
        DataExternalizer<Collection<ModuleInfoValue>> {
        override fun read(input: DataInput): Collection<ModuleInfoValue> {
            val size = input.readInt()
            val modules = ArrayList<ModuleInfoValue>(size)
            repeat(size) {
                modules.add(readModuleInfo(input))
            }
            return modules
        }

        override fun save(output: DataOutput, modules: Collection<ModuleInfoValue>) {
            output.writeInt(modules.size)
            modules.forEach { saveModuleInfo(output, it) }
        }

        fun readModuleInfo(input: DataInput): ModuleInfoValue = with(input) {
            val name = input.readUTF()
            val filePath = input.readUTF()
            val fileContent = input.readLongUTF()!!
            val moduleVariable = input.readUTF()
            val kotlinVariable = input.readUTF()
            val specialFunctionsSize = input.readInt()
            val specialFunctions = HashMap<String, SpecialFunction>(specialFunctionsSize)
            repeat(specialFunctionsSize) {
                val k = readUTF()
                val v = readInt()
                specialFunctions[k] = SpecialFunction.values()[v]
            }
            val hasSourceMap = readBoolean()
            val sourceMap = if (hasSourceMap) readSourceMap() else null
            val outputDir = readNullableUTF()?.let(::File)

            ModuleInfoValue(
                name = name,
                filePath = filePath,
                fileContent = fileContent,
                moduleVariable = moduleVariable,
                kotlinVariable = kotlinVariable,
                specialFunctions = specialFunctions,
                sourceMap = sourceMap,
                outputDir = outputDir
            )
        }

        fun saveModuleInfo(output: DataOutput, module: ModuleInfoValue) {
            with(output) {
                writeUTF(module.name)
                writeUTF(module.filePath)
                writeLongUTF(module.fileContent)
                writeUTF(module.moduleVariable)
                writeUTF(module.kotlinVariable)
                writeInt(module.specialFunctions.size)
                module.specialFunctions.forEach { (k, v) ->
                    writeUTF(k)
                    writeInt(v.ordinal)
                }
                val sourceMap = module.sourceMap
                writeBoolean(sourceMap != null)
                if (sourceMap != null) {
                    saveSourceMap(sourceMap)
                }
                writeNullableUTF(module.outputDir?.canonicalPath)
            }
        }

        private fun DataInput.readSourceMap(): SourceMap {
            val numberOfGroups = readInt()
            val groups = ArrayList<SourceMapGroup>(numberOfGroups)
            repeat(numberOfGroups) {
                groups.add(readSourceMapGroup())
            }
            val contentSize = readInt()
            val content = HashMap<String, String?>(contentSize)
            repeat(contentSize) {
                val k = readUTF()
                val v = readLongUTF()
                content[k] = v
            }
            return SourceMap(content, groups)
        }

        private fun DataOutput.saveSourceMap(sourceMap: SourceMap) {
            val groups = sourceMap.groups
            writeInt(groups.size)
            groups.forEach { saveSourceMapGroup(it) }
            val content = sourceMap.sourceContent
            writeInt(content.size)
            content.forEach { k, v ->
                writeUTF(k)
                writeLongUTF(v)
            }
        }

        private fun DataInput.readSourceMapGroup(): SourceMapGroup {
            val size = readInt()
            val segments = ArrayList<SourceMapSegment>(size)
            repeat(size) {
                segments.add(readSourceMapSegment())
            }
            return SourceMapGroup(segments)
        }

        private fun DataOutput.saveSourceMapGroup(group: SourceMapGroup) {
            writeInt(group.segments.size)
            group.segments.forEach { segment ->
                saveSourceMapSegment(segment)
            }
        }

        private fun DataInput.readSourceMapSegment(): SourceMapSegment {
            val sourceLineNumber = readInt()
            val sourceColumnNumber = readInt()
            val generatedColumnNumber = readInt()
            val sourceFileName = readNullableUTF()
            return SourceMapSegment(
                sourceLineNumber = sourceLineNumber,
                sourceColumnNumber = sourceColumnNumber,
                generatedColumnNumber = generatedColumnNumber,
                sourceFileName = sourceFileName
            )
        }

        private fun DataOutput.saveSourceMapSegment(segment: SourceMapSegment) {
            writeInt(segment.sourceLineNumber)
            writeInt(segment.sourceColumnNumber)
            writeInt(segment.generatedColumnNumber)
            writeNullableUTF(segment.sourceFileName)
        }

        private fun DataInput.readNullableUTF(): String? {
            val value = readBoolean()
            return if (value) readUTF() else null
        }

        private fun DataOutput.writeNullableUTF(value: String?) {
            val hasValue = value != null
            writeBoolean(hasValue)
            if (hasValue) {
                writeUTF(value)
            }
        }

        private fun DataInput.readLongUTF(): String? {
            val size = readInt()
            if (size < 0) return null

            val bytes = ByteArray(size)
            readFully(bytes)
            return bytes.toString(Charsets.UTF_8)
        }

        private fun DataOutput.writeLongUTF(value: String?) {
            if (value == null) {
                writeInt(-1)
                return
            }

            val bytes = value.toByteArray(Charsets.UTF_8)
            writeInt(bytes.size)
            write(bytes)
        }
    }
}