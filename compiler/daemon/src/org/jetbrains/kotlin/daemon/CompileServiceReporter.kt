/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter

// For messages of about compile daemon
abstract class CompileServiceReporter {
    fun info(message: String) {
        report(CompilerMessageSeverity.INFO, message)
    }

    fun exception(e: Exception) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        sw.toString()
        report(CompilerMessageSeverity.EXCEPTION, sw.toString())
    }

    protected abstract fun report(severity: CompilerMessageSeverity, message: String)

    object NONE : CompileServiceReporter() {
        override fun report(severity: CompilerMessageSeverity, message: String) {}
    }
}

class CompileServiceReporterStreamAdapter(private val out: PrintStream) : CompileServiceReporter() {
    override fun report(severity: CompilerMessageSeverity, message: String) {
        out.print("[Kotlin daemon][$severity] $message")
    }
}