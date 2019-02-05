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

package org.jetbrains.kotlin.daemon.report

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.ICReporter
import java.io.File
import java.util.*

internal abstract class RemoteICReporter : ICReporter {
    open fun flush() {}
}

internal fun getICReporter(
    servicesFacade: CompilerServicesFacadeBase,
    compilationResults: CompilationResults,
    compilationOptions: IncrementalCompilationOptions
): RemoteICReporter {
    val reporters = ArrayList<RemoteICReporter>()

    if (ReportCategory.IC_MESSAGE.code in compilationOptions.reportCategories) {
        reporters.add(DebugMessagesICReporter(servicesFacade, compilationOptions))
    }

    val requestedResults = compilationOptions
        .requestedCompilationResults
        .mapNotNullTo(HashSet()) { resultCode ->
            CompilationResultCategory.values().getOrNull(resultCode)
        }
    requestedResults.mapTo(reporters) { requestedResult ->
        when (requestedResult) {
            CompilationResultCategory.IC_COMPILE_ITERATION -> {
                CompileIterationICReporter(compilationResults)
            }
            CompilationResultCategory.BUILD_REPORT_LINES -> {
                BuildReportICReporter(compilationResults, compilationOptions.modulesInfo.projectRoot)
            }
            CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES -> {
                BuildReportICReporter(compilationResults, compilationOptions.modulesInfo.projectRoot, isVerbose = true)
            }
        }
    }

    return CompositeICReporter(reporters)
}

private class CompileIterationICReporter(
    private val compilationResults: CompilationResults
) : RemoteICReporter() {
    override fun report(message: () -> String) {
    }

    override fun reportVerbose(message: () -> String) {
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        compilationResults.add(
            CompilationResultCategory.IC_COMPILE_ITERATION.code,
            CompileIterationResult(sourceFiles, exitCode.toString())
        )
    }
}

private class DebugMessagesICReporter(
    private val servicesFacade: CompilerServicesFacadeBase,
    compilationOptions: IncrementalCompilationOptions
) : RemoteICReporter() {
    private val isVerbose = compilationOptions.reportSeverity == ReportSeverity.DEBUG.code

    override fun report(message: () -> String) {
        servicesFacade.report(ReportCategory.IC_MESSAGE, ReportSeverity.DEBUG, message())
    }

    override fun reportVerbose(message: () -> String) {
        if (isVerbose) {
            report(message)
        }
    }
}

private class BuildReportICReporter(
    private val compilationResults: CompilationResults,
    private val rootDir: File,
    private val isVerbose: Boolean = false
) : RemoteICReporter() {

    private val icLogLines = arrayListOf<String>()
    private val recompilationReason = HashMap<File, String>()

    override fun report(message: () -> String) {
        icLogLines.add(message())
    }

    override fun reportVerbose(message: () -> String) {
        if (isVerbose) {
            report(message)
        }
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        if (!incremental) return

        icLogLines.add("Compile iteration:")
        sourceFiles.relativePaths(rootDir).forEach { file ->
            val reason = recompilationReason[file]?.let { " <- $it" } ?: ""
            icLogLines.add("  $file$reason")
        }
        recompilationReason.clear()
    }

    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {
        affectedFiles.forEach { recompilationReason[it] = reason }
    }

    override fun flush() {
        compilationResults.add(CompilationResultCategory.BUILD_REPORT_LINES.code, icLogLines)
    }
}

private class CompositeICReporter(private val reporters: Iterable<ICReporter>) : RemoteICReporter() {
    override fun report(message: () -> String) {
        reporters.forEach { it.report(message) }
    }

    override fun reportVerbose(message: () -> String) {
        reporters.forEach { it.reportVerbose(message) }
    }
}

private fun File.relativeOrCanonical(base: File): String =
    relativeToOrNull(base)?.path ?: canonicalPath

private fun Iterable<File>.relativePaths(base: File): List<String> =
    map { it.relativeOrCanonical(base) }.sorted()

