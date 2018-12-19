/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.js

import java.io.File

interface ModuleInfoCache {
    operator fun get(file: File): Collection<ModuleInfoValue>?
    operator fun set(file: File, moduleInfoValue: Collection<ModuleInfoValue>)
    fun flush()

    object Empty : ModuleInfoCache {
        override fun get(file: File): Collection<ModuleInfoValue>? = null
        override fun set(file: File, moduleInfoValue: Collection<ModuleInfoValue>) {}
        override fun flush() {}
    }
}