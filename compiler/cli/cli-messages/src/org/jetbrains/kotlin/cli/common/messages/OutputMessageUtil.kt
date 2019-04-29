/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.common.messages

import java.io.File
import java.io.PrintWriter
import java.io.Serializable
import java.io.StringWriter

object OutputMessageUtil {
    private const val SOURCE_FILES_PREFIX = "Sources:"
    private const val OUTPUT_FILES_PREFIX = "Output:"

    fun renderException(e: Throwable): String {
        val out = StringWriter()
        e.printStackTrace(PrintWriter(out))
        return out.toString()
    }

    fun formatOutputMessage(sourceFiles: Collection<File>, outputFile: File): String =
        OUTPUT_FILES_PREFIX + "\n" + outputFile.path + "\n" + SOURCE_FILES_PREFIX + "\n" + sourceFiles.joinToString("\n")

    fun parseOutputMessage(message: String): Output? {
        val strings = message.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        // Must have at least one line per prefix
        if (strings.size <= 2) return null

        if (OUTPUT_FILES_PREFIX != strings[0]) return null

        if (SOURCE_FILES_PREFIX == strings[1]) {
            // Output:
            // Sources:
            // ...
            return Output(parseSourceFiles(strings, 2), null)
        } else {
            val outputFile = File(strings[1])

            return if (SOURCE_FILES_PREFIX != strings[2]) null else Output(parseSourceFiles(strings, 3), outputFile)
        }
    }

    private fun parseSourceFiles(strings: Array<String>, start: Int): Collection<File> {
        val sourceFiles = arrayListOf<File>()
        for (i in start until strings.size) {
            sourceFiles.add(File(strings[i]))
        }
        return sourceFiles
    }

    class Output(val sourceFiles: Collection<File>, val outputFile: File?) : Serializable {
        companion object {

            internal const val serialVersionUID = 0L
        }
    }
}
