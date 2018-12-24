/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.js

import java.io.File

class InMemoryModuleInfoCacheImpl : ModuleInfoCache {
    private class CachedValue(val crc: Long, val value: Collection<ModuleInfoValue>)

    private val cachedValues = HashMap<File, CachedValue>()

    @Synchronized
    override fun get(file: File): Collection<ModuleInfoValue>? {
        val value = cachedValues[file] ?: return null
        if (value.crc != file.crc) {
            cachedValues.remove(file)
            return null
        }
        return value.value
    }

    @Synchronized
    override fun set(file: File, moduleInfoValue: Collection<ModuleInfoValue>) {
        cachedValues[file] = CachedValue(file.crc, moduleInfoValue)
    }

    @Synchronized
    fun remove(file: File) {
        cachedValues.remove(file)
    }

    private val File.crc: Long
        get() = lastModified() * 31 + length()

    override fun flush() {
        //super.flush(memoryCachesOnly = false)
        //saveCurrentVersion(cacheVersionFile)
    }
}