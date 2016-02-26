package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

open class K2JVMGradleCompilerArguments : K2JVMCompilerArguments() {
    var experimentalIncremental: Boolean = false
}