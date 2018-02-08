/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.multiproject

import org.jetbrains.kotlin.incremental.*
import java.io.File

class ChangesRegistryImpl(private val file: File, private val reporter: ICReporter?) : ChangesRegistry {
    override fun registerChanges(timestamp: Long, dirtyData: DirtyData) {
        val diffs = BuildDiffsStorage.readDiffsFromFile(file, reporter) ?: arrayListOf()
        diffs.add(BuildDifference(timestamp, true, dirtyData))
        BuildDiffsStorage.writeToFile(file, BuildDiffsStorage(diffs), reporter = null)
    }

    override fun unknownChanges(timestamp: Long) {
        file.delete()
        val emptyDirtyData = DirtyData()
        val diffs = listOf(BuildDifference(timestamp, false, emptyDirtyData))
        BuildDiffsStorage.writeToFile(file, BuildDiffsStorage(diffs), reporter = null)
    }
}