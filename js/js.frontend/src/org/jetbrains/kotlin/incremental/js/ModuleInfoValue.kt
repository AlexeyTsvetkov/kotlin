/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.js

import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSegment
import java.io.File
import java.io.Serializable

data class ModuleInfoValue(
    val name: String,
    val filePath: String,
    val fileContent: String,
    val moduleVariable: String,
    val kotlinVariable: String,
    val specialFunctions: Map<String, SpecialFunction>,
    val sourceMap: Collection<Collection<SourceMapSegment>>?,
    val outputDir: File?
) : Serializable {
    companion object {
        private const val serialVersionUID = 0
    }
}