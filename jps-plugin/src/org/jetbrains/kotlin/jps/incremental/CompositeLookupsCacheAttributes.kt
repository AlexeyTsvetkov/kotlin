/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.kotlin.incremental.storage.version.CacheAttributesManager
import org.jetbrains.kotlin.incremental.storage.version.CacheVersion
import org.jetbrains.kotlin.incremental.storage.version.CacheVersionManager
import org.jetbrains.kotlin.incremental.storage.version.lookupsCacheVersionManager
import java.io.File
import java.io.IOException

/**
 * Attributes manager for global lookups cache that may contain lookups for several compilers (jvm, js).
 * Works by delegating to [lookupsCacheVersionManager] and managing additional file with list of executed compilers (cache components).
 *
 * TODO(1.2.80): got rid of shared lookup cache, replace with individual lookup cache for each compiler
 */
class CompositeLookupsCacheAttributesManager private constructor(
    private val actualComponentsFile: File,
    expectedComponents: Set<String>,
    val versionManager: CacheVersionManager
) : CacheAttributesManager<CompositeLookupsCacheAttributes>(
    expected = loadExpected(expectedComponents, versionManager),
    actual = loadActual(actualComponentsFile, versionManager)
) {
    constructor(rootPath: File, expectedComponents: Set<String>) :
            this(
                File(rootPath, "components.txt"),
                expectedComponents,
                lookupsCacheVersionManager(rootPath, expectedComponents.isNotEmpty())
            )

    override fun writeActualVersion(values: CompositeLookupsCacheAttributes?) {
        super.writeActualVersion(values)
        if (values == null) {
            versionManager.writeActualVersion(null)
            actualComponentsFile.delete()
        } else {
            versionManager.writeActualVersion(CacheVersion(values.version))

            actualComponentsFile.parentFile.mkdirs()
            actualComponentsFile.writeText(values.components.joinToString("\n"))
        }
    }

    override fun isCompatible(actual: CompositeLookupsCacheAttributes, expected: CompositeLookupsCacheAttributes): Boolean {
        // cache can be reused when all required (expected) components are present
        // (components that are not required anymore are not not interfere)
        return actual.version == expected.version && actual.components.containsAll(expected.components)
    }

    companion object {
        private fun loadExpected(
            expectedComponents: Set<String>,
            versionManager: CacheVersionManager
        ): CompositeLookupsCacheAttributes? {
            if (expectedComponents.isEmpty()) return null

            return CompositeLookupsCacheAttributes(versionManager.expected!!.version, expectedComponents)
        }

        private fun loadActual(actualComponentsFile: File, versionManager: CacheVersionManager): CompositeLookupsCacheAttributes? {
            val version = versionManager.actual ?: return null

            if (!actualComponentsFile.exists()) return null

            val components = try {
                actualComponentsFile.readLines().toSet()
            } catch (e: IOException) {
                return null
            }

            return CompositeLookupsCacheAttributes(version.version, components)
        }
    }
}

data class CompositeLookupsCacheAttributes(
    val version: Int,
    val components: Set<String>
) {
    override fun toString() = "($version, $components)"
}