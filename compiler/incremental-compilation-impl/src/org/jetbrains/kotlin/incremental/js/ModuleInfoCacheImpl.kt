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
                modules.add(ModuleInfoValue.readModuleInfo(input))
            }
            return modules
        }

        override fun save(output: DataOutput, modules: Collection<ModuleInfoValue>) {
            output.writeInt(modules.size)
            modules.forEach { ModuleInfoValue.saveModuleInfo(output, it) }
        }
    }
}