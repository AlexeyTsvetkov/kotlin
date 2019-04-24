/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

import java.io.Serializable

data class CompilerMessageLocation private constructor(
    val path: String,
    val line: Int,
    val column: Int,
    val lineContent: String?
) : Serializable {
    override fun toString(): String =
        path + (if (line != -1 || column != -1) " ($line:$column)" else "")

    companion object {
        @JvmStatic
        fun create(path: String?): CompilerMessageLocation? =
            create(path, -1, -1, null)

        @JvmStatic
        fun create(path: String?, line: Int, column: Int, lineContent: String?): CompilerMessageLocation? =
            if (path == null) null else CompilerMessageLocation(path, line, column, lineContent)

        private val serialVersionUID: Long = 8228357578L
    }
}
