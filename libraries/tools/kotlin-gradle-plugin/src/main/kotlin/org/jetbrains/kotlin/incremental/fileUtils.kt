package org.jetbrains.kotlin.incremental

import java.io.File

internal fun File.isJavaFile() =
        extension.equals("java", ignoreCase = true)

internal fun File.isKotlinFile(): Boolean =
    extension.let {
        "kt".equals(it, ignoreCase = true) ||
        "kts".equals(it, ignoreCase = true)
    }

internal fun File.isClassFile(): Boolean =
        extension.equals("class", ignoreCase = true)

internal fun listClassFiles(path: String): Sequence<File> =
        File(path).walk().filter { it.isFile && it.isClassFile() }

internal fun File.relativeOrCanonical(base: File): String =
        relativeToOrNull(base)?.path ?: canonicalPath

internal fun Iterable<File>.pathsAsStringRelativeTo(base: File): String =
        "[" + map { it.relativeOrCanonical(base) }.sorted().joinToString(separator = ", \n") + "]"
