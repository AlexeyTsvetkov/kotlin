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

package org.jetbrains.kotlin.compilerRunner

import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.jps.api.GlobalOptions
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.TimeUnit

class JpsKotlinCompilerRunner : KotlinCompilerRunner<JpsCompilerEnvironment>() {
    companion object {
        private @Volatile var jpsDaemonConnection: DaemonConnection? = null
    }

    fun runK2JvmCompiler(
            commonArguments: CommonCompilerArguments,
            k2jvmArguments: K2JVMCompilerArguments,
            compilerSettings: CompilerSettings,
            messageCollector: MessageCollector,
            environment: JpsCompilerEnvironment,
            moduleFile: File,
            collector: OutputItemsCollector
    ) {
        val arguments = mergeBeans(commonArguments, k2jvmArguments)
        setupK2JvmArguments(moduleFile, arguments)

        runCompiler(K2JVM_COMPILER, arguments, compilerSettings.additionalArguments, messageCollector, collector, environment)
    }

    fun runK2JsCompiler(
            commonArguments: CommonCompilerArguments,
            k2jsArguments: K2JSCompilerArguments,
            compilerSettings: CompilerSettings,
            messageCollector: MessageCollector,
            environment: JpsCompilerEnvironment,
            collector: OutputItemsCollector,
            sourceFiles: Collection<File>,
            libraryFiles: List<String>,
            outputFile: File
    ) {
        val arguments = mergeBeans(commonArguments, k2jsArguments)
        setupK2JsArguments(outputFile, sourceFiles, libraryFiles, arguments)

        runCompiler(K2JS_COMPILER, arguments, compilerSettings.additionalArguments, messageCollector, collector, environment)
    }

    private fun setupK2JvmArguments(moduleFile: File, settings: K2JVMCompilerArguments) {
        with(settings) {
            module = moduleFile.absolutePath
            noStdlib = true
            noReflect = true
            noJdk = true
        }
    }

    private fun setupK2JsArguments(_outputFile: File, sourceFiles: Collection<File>, _libraryFiles: List<String>, settings: K2JSCompilerArguments) {
        with(settings) {
            noStdlib = true
            freeArgs = sourceFiles.map { it.path }
            outputFile = _outputFile.path
            metaInfo = true
            libraryFiles = _libraryFiles.toTypedArray()
        }
    }

    private fun <T : CommonCompilerArguments> mergeBeans(from: CommonCompilerArguments, to: T): T {
        // TODO: rewrite when updated version of com.intellij.util.xmlb is available on TeamCity
        val copy = XmlSerializerUtil.createCopy(to)

        val fromFields = collectFieldsToCopy(from.javaClass)
        for (fromField in fromFields) {
            val toField = copy.javaClass.getField(fromField.name)
            toField.set(copy, fromField.get(from))
        }

        return copy
    }

    private fun collectFieldsToCopy(clazz: Class<*>): List<Field> {
        val fromFields = ArrayList<Field>()

        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            for (field in currentClass.declaredFields) {
                val modifiers = field.modifiers
                if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                    fromFields.add(field)
                }
            }
            currentClass = currentClass.superclass
        }

        return fromFields
    }

    private fun reportTotalAndThreadPerf(message: String, daemonOptions: DaemonOptions, messageCollector: MessageCollector, profiler: Profiler) {
        if (daemonOptions.reportPerf) {
            fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
            val counters = profiler.getTotalCounters()
            messageCollector.report(CompilerMessageSeverity.INFO,
                                    "PERF: $message ${counters.time.ms()} ms, thread ${counters.threadTime.ms()}",
                                    CompilerMessageLocation.NO_LOCATION)
        }
    }

    override fun fallbackCompileStrategy(
            argsArray: Array<String>,
            collector: OutputItemsCollector,
            compilerClassName: String,
            environment: JpsCompilerEnvironment,
            messageCollector: MessageCollector
    ) {
        // otherwise fallback to in-process
        logInfo("Compile in-process")

        val stream = ByteArrayOutputStream()
        val out = PrintStream(stream)

        // the property should be set at least for parallel builds to avoid parallel building problems (racing between destroying and using environment)
        // unfortunately it cannot be currently set by default globally, because it breaks many tests
        // since there is no reliable way so far to detect running under tests, switching it on only for parallel builds
        if (System.getProperty(GlobalOptions.COMPILE_PARALLEL_OPTION, "false").toBoolean())
            System.setProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")

        val rc = CompilerRunnerUtil.invokeExecMethod(compilerClassName, argsArray, environment, messageCollector, out)

        // exec() returns an ExitCode object, class of which is loaded with a different class loader,
        // so we take it's contents through reflection
        processCompilerOutput(messageCollector, collector, stream, getReturnCodeFromObject(rc))
    }

    private fun getReturnCodeFromObject(rc: Any?): String {
        when {
            rc == null -> return INTERNAL_ERROR
            ExitCode::class.java.name == rc.javaClass.name -> return rc.toString()
            else -> throw IllegalStateException("Unexpected return: " + rc)
        }
    }

    override fun getDaemonConnection(environment: JpsCompilerEnvironment, messageCollector: MessageCollector): DaemonConnection {
        if (jpsDaemonConnection == null) {
            val libPath = CompilerRunnerUtil.getLibPath(environment.kotlinPaths, messageCollector)
            val compilerId = CompilerId.makeCompilerId(File(libPath, "kotlin-compiler.jar"))
            val daemonOptions = configureDaemonOptions()
            val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = true, inheritAdditionalProperties = true)

            val daemonReportMessages = ArrayList<DaemonReportMessage>()

            val profiler = if (daemonOptions.reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

            profiler.withMeasure(null) {
                fun newFlagFile(): File {
                    val flagFile = File.createTempFile("kotlin-compiler-jps-session-", "-is-running")
                    flagFile.deleteOnExit()
                    return flagFile
                }

                val daemon = KotlinCompilerClient.connectToCompileService(compilerId, daemonJVMOptions, daemonOptions, DaemonReportingTargets(null, daemonReportMessages), true, true)
                jpsDaemonConnection = DaemonConnection(daemon, daemon?.leaseCompileSession(newFlagFile().absolutePath)?.get() ?: CompileService.NO_SESSION)
            }

            for (msg in daemonReportMessages) {
                messageCollector.report(CompilerMessageSeverity.INFO,
                                        (if (msg.category == DaemonReportCategory.EXCEPTION && jpsDaemonConnection?.daemon == null) "Falling  back to compilation without daemon due to error: " else "") + msg.message,
                                        CompilerMessageLocation.NO_LOCATION)
            }

            reportTotalAndThreadPerf("Daemon connect", daemonOptions, messageCollector, profiler)
        }
        return jpsDaemonConnection!!
    }

    override fun logInfo(msg: String) {
        KotlinBuilder.LOG.info(msg)
    }

    override fun logDebug(msg: String) {
        KotlinBuilder.LOG.debug(msg)
    }
}