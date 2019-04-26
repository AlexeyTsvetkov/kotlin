package org.jetbrains.kotlin.modules

import org.jetbrains.kotlin.build.JvmSourceRoot
import java.io.File

const val DELETE_MODULE_FILE_PROPERTY = "kotlin.delete.module.file.after.build"

fun makeModuleFile(
    name: String,
    isTest: Boolean,
    outputDir: File,
    sourcesToCompile: Iterable<File>,
    commonSources: Iterable<File>,
    javaSourceRoots: Iterable<JvmSourceRoot>,
    classpath: Iterable<File>,
    friendDirs: Iterable<File>
): File {
    val builder = KotlinModuleXmlBuilder()
    builder.addModule(
        name,
        outputDir.absolutePath,
        // important to transform file to absolute paths,
        // otherwise compiler will use module file's parent as base path (a temporary file; see below)
        // (see org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.getAbsolutePaths)
        sourcesToCompile.map { it.absoluteFile },
        javaSourceRoots,
        classpath,
        commonSources.map { it.absoluteFile },
        null,
        "java-production",
        isTest,
        // this excludes the output directories from the class path, to be removed for true incremental compilation
        setOf(outputDir),
        friendDirs
    )

    val scriptFile = File.createTempFile("kjps", sanitizeJavaIdentifier(name) + ".script.xml")
    scriptFile.writeText(builder.asText().toString())
    return scriptFile
}

private fun sanitizeJavaIdentifier(string: String) =
    buildString {
        for (char in string) {
            if (char.isJavaIdentifierPart()) {
                if (length == 0 && !char.isJavaIdentifierStart()) {
                    append('_')
                }
                append(char)
            }
        }
    }