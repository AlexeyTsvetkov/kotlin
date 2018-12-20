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

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.js.ModuleInfoCache
import org.jetbrains.kotlin.incremental.js.ModuleInfoValue
import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.PathStringDescriptor
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import java.io.DataInput
import java.io.DataOutput
import java.io.File

abstract class IncrementalCachesManager<PlatformCache : AbstractIncrementalCache<*>>(
    protected val cachesRootDir: File,
    protected val reporter: ICReporter
) {
    private val caches = arrayListOf<BasicMapsOwner>()
    protected fun <T : BasicMapsOwner> T.registerCache() {
        caches.add(this)
    }

    private val inputSnapshotsCacheDir = File(cachesRootDir, "inputs").apply { mkdirs() }
    private val lookupCacheDir = File(cachesRootDir, "lookups").apply { mkdirs() }

    val inputsCache: InputsCache = InputsCache(inputSnapshotsCacheDir, reporter).apply { registerCache() }
    val lookupCache: LookupStorage = LookupStorage(lookupCacheDir).apply { registerCache() }
    abstract val platformCache: PlatformCache

    fun clean() {
        caches.forEach { it.clean() }
        cachesRootDir.deleteRecursively()
    }

    fun close(flush: Boolean = false): Boolean {
        var successful = true

        for (cache in caches) {
            if (flush) {
                try {
                    cache.flush(false)
                }
                catch (e: Throwable) {
                    successful = false
                    reporter.report { "Exception when flushing cache ${cache.javaClass}: $e" }
                }
            }

            try {
                cache.close()
            }
            catch (e: Throwable) {
                successful = false
                reporter.report { "Exception when closing cache ${cache.javaClass}: $e" }
            }
        }

        return successful
    }
}

class IncrementalJvmCachesManager(
    cacheDirectory: File,
    outputDir: File,
    reporter: ICReporter
) : IncrementalCachesManager<IncrementalJvmCache>(cacheDirectory, reporter) {

    private val jvmCacheDir = File(cacheDirectory, "jvm").apply { mkdirs() }
    override val platformCache = IncrementalJvmCache(jvmCacheDir, outputDir).apply { registerCache() }
}

class IncrementalJsCachesManager(
        cachesRootDir: File,
        reporter: ICReporter
) : IncrementalCachesManager<IncrementalJsCache>(cachesRootDir, reporter) {

    private val jsCacheFile = File(cachesRootDir, "js").apply { mkdirs() }
    override val platformCache = IncrementalJsCache(jsCacheFile).apply { registerCache() }
    val moduleInfo = ModuleInfoCacheImpl(jsCacheFile).apply { registerCache() }
}

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


    override fun get(file: File): Collection<ModuleInfoValue>? {
        moduleInfoMap.get(file)
    }

    override fun set(file: File, moduleInfoValue: Collection<ModuleInfoValue>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
    ) : BasicStringMap<Collection<ModuleInfoValue>>(storageFile, PathStringDescriptor, ModuleInfoCollectionExternalizer) {
        override fun dumpValue(value: Collection<ModuleInfoValue>): String {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    private object ModuleInfoCollectionExternalizer : DataExternalizer<Collection<ModuleInfoValue>> {
        override fun read(input: DataInput): Collection<ModuleInfoValue> {
            val size = input.readInt()
            val modules = ArrayList<ModuleInfoValue>(size)
            repeat(size) {
                modules.add(ModuleInfoValue.readModuleInfo(input))
            }
            return modules
        }

        override fun save(output: DataOutput, modules: Collection<ModuleInfoValue>) {
            output.write(modules.size)
            modules.forEach { ModuleInfoValue.saveModuleInfo(output, it) }
        }
    }
}