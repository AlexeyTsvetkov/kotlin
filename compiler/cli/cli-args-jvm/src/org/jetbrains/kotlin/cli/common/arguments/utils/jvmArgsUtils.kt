package org.jetbrains.kotlin.cli.common.arguments.utils

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.File

var K2JVMCompilerArguments.destinationAsFile: File
    get() = File(destination)
    set(value) { destination = value.path }

var K2JVMCompilerArguments.classpathAsList: List<File>
    get() = classpath.orEmpty().split(File.pathSeparator).map(::File)
    set(value) { classpath = value.joinToString(separator = File.pathSeparator, transform = { it.path }) }