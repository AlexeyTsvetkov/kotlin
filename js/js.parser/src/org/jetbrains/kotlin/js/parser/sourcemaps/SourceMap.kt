/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.parser.sourcemaps

import java.io.File
import java.io.PrintStream

class SourceMap(val sourceContent: Map<String, String?>, private val myGroups: MutableList<SourceMapGroup> = arrayListOf()) {
    val groups: List<SourceMapGroup>
        get() = myGroups

    fun addGroup(segments: List<SourceMapSegment>) {
        val group = if (segments.isEmpty()) EmptySourceMapGroup else SourceMapGroup(segments)
        myGroups.add(group)
    }

    fun debug(writer: PrintStream = System.out) {
        for ((index, group) in groups.withIndex()) {
            writer.print("${index + 1}:")
            for (segment in group.segments) {
                writer.print(" ${segment.generatedColumnNumber + 1}:${segment.sourceLineNumber + 1},${segment.sourceColumnNumber + 1}")
            }
            writer.println()
        }
    }

    fun debugVerbose(writer: PrintStream, generatedJsFile: File) {
        assert(generatedJsFile.exists()) { "$generatedJsFile does not exist!" }
        val generatedLines = generatedJsFile.readLines().toTypedArray()
        for ((index, group) in groups.withIndex()) {
            writer.print("${index + 1}:")
            val generatedLine = generatedLines[index]
            val segmentsByColumn = group.segments.map { it.generatedColumnNumber to it }.toMap()
            for (i in generatedLine.indices) {
                segmentsByColumn[i]?.let { (_, sourceFile, sourceLine, sourceColumn) ->
                    writer.print("<$sourceFile:${sourceLine + 1}:${sourceColumn + 1}>")
                }
                writer.print(generatedLine[i])
            }
            writer.println()
        }
    }
}

data class SourceMapSegment(
    val generatedColumnNumber: Int,
    val sourceFileName: String?,
    val sourceLineNumber: Int,
    val sourceColumnNumber: Int
)

open class SourceMapGroup(val segments: List<SourceMapSegment>)

object EmptySourceMapGroup : SourceMapGroup(emptyList())

sealed class SourceMapParseResult

class SourceMapSuccess(val value: SourceMap) : SourceMapParseResult()

class SourceMapError(val message: String) : SourceMapParseResult()