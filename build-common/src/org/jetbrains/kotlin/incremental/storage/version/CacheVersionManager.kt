/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage.version

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import java.io.File
import java.io.IOException

/**
 * Manages files with actual version [loadActual] and provides expected version [expected].
 * Based on that actual and expected versions [CacheStatus] can be calculated.
 * This can be done by constructing [CacheAttributesDiff] and calling [CacheAttributesDiff.status].
 * Based on that status system may perform required actions (i.e. rebuild something, clearing caches, etc...).
 */
class CacheVersionManager(
    @get:TestOnly
    val versionFile: File,
    expectedOwnVersion: Int?
) : CacheAttributesManager<CacheVersion>(
    expected = loadExpected(ownVersion = expectedOwnVersion),
    actual = loadActual(versionFile)
) {
    override fun isCompatible(actual: CacheVersion, expected: CacheVersion): Boolean =
        actual == expected

    override fun writeActualVersion(values: CacheVersion?) {
        super.writeActualVersion(values)
        if (values == null) versionFile.delete()
        else {
            versionFile.parentFile.mkdirs()
            versionFile.writeText(values.version.toString())
        }
    }

    companion object {
        private fun loadExpected(ownVersion: Int?): CacheVersion? =
            ownVersion?.let { ownVersion ->
                val metadata = JvmMetadataVersion.INSTANCE
                val bytecode = JvmBytecodeBinaryVersion.INSTANCE

                CacheVersion(
                    ownVersion * 1000000 +
                            bytecode.major * 10000 + bytecode.minor * 100 +
                            metadata.major * 1000 + metadata.minor
                )
            }

        private fun loadActual(versionFile: File): CacheVersion? =
            if (!versionFile.exists()) null
            else try {
                CacheVersion(versionFile.readText().toInt())
            } catch (e: NumberFormatException) {
                null
            } catch (e: IOException) {
                null
            }
    }
}

data class CacheVersion(val version: Int)