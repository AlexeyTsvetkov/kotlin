package org.jetbrains.kotlin.gradle.util

import java.io.File
import java.util.*

fun File.getFileByName(name: String): File =
        findFileByName(name) ?: throw AssertionError("Could not find file with name '$name' in $this")

fun File.findFileByName(name: String): File? =
        walk().filter { it.isFile && it.name.equals(name, ignoreCase = true) }.firstOrNull()

fun File.allKotlinFiles(): Iterable<File> =
        walk().filter { it.isFile && it.extension.toLowerCase().equals("kt") }.toList()

fun File.modify(transform: (String)->String) {
    writeText(transform(readText()))
}

/**
 * Puts all [newProperties] in *.properties file
 */
fun File.putAllProperties(newProperties: Map<String, String>) {
    assert(extension.equals("properties", ignoreCase = true)) { "Invalid file extension, expected '.properties'" }
    val properties = Properties()

    if (isFile) {
        bufferedReader().use { reader ->
            properties.load(reader)
        }
    }

    properties.putAll(newProperties)
    bufferedWriter().use { writer ->
        properties.store(writer, /* comments = */"")
    }
}