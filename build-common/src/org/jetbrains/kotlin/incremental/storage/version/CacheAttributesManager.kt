/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage.version

/**
 * Manages cache attributes values.
 *
 * Attribute values can be loaded by calling [loadActual].
 * Based on loaded actual and fixed [expected] values [CacheAttributesDiff] can be constructed which can calculate [CacheStatus].
 * Build system may perform required actions based on that (i.e. rebuild something, clearing caches, etc...).
 *
 * [CacheAttributesDiff] can be used to cache current attribute values and then can be used as facade for cache version operations.
 */
abstract class CacheAttributesManager<Attrs : Any>(val expected: Attrs?, actual: Attrs?) {
    protected abstract fun isCompatible(actual: Attrs, expected: Attrs): Boolean

    var actual: Attrs? = actual
        private set

    open fun writeActualVersion(values: Attrs?) {
        actual = values
    }

    val status: CacheStatus
        get() {
            val expected = expected
            val actual = actual

            return if (expected != null) {
                if (actual != null && isCompatible(actual, expected)) CacheStatus.VALID
                else CacheStatus.INVALID
            } else {
                if (actual != null) CacheStatus.SHOULD_BE_CLEARED
                else CacheStatus.VALID
            }
        }

    fun saveExpectedIfNeeded() {
        if (expected != actual) writeActualVersion(expected)
    }

    fun clean() {
        writeActualVersion(null)
    }
}