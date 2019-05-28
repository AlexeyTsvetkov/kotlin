/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

import java.io.File

/**
 * Calculates the relative path to this file from [base] file.
 * Note that the [base] file is treated as a directory.
 *
 * If this file matches the [base] directory an empty path is returned.
 * If this file does not belong to the [base] directory, it is returned unchanged.
 */
internal fun File.descendantRelativeTo(base: File): File {
    val prefix = base.canonicalPath
    val answer = this.canonicalPath
    return if (answer.startsWith(prefix)) {
        val prefixSize = prefix.length
        if (answer.length > prefixSize) {
            File(answer.substring(prefixSize + 1))
        } else File("")
    } else {
        this
    }
}