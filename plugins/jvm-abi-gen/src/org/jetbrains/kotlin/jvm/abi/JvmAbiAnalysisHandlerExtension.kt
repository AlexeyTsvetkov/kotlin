/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.jvm.abi.asm.*
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*


private interface MethodsSet {
    fun contains(name: String, desc: String): Boolean
}

private class MutableMethodsSet : MethodsSet {
    private val methods = HashSet<String>()

    override fun contains(name: String, desc: String): Boolean =
        (name + desc) in methods

    fun add(method: MethodSignatureInfo) {
        add(name = method.name, desc = method.desc)
    }

    fun add(name: String, desc: String) {
        methods.add(name + desc)
    }
}

private class InlineFunctionsRegistry {
    private val inlineFuns = HashMap<String, MutableMethodsSet>()

    fun add(method: MethodSignatureInfo) {
        inlineFuns.getOrPut(method.owner) { MutableMethodsSet() }.add(method)
    }

    fun getInlineFuns(owner: String): MethodsSet? =
        inlineFuns[owner]

    val owners: Collection<String> =
        inlineFuns.keys
}

class JvmAbiAnalysisHandlerExtension(
    private val compilerConfiguration: CompilerConfiguration
) : AnalysisHandlerExtension {
    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        val bindingContext = bindingTrace.bindingContext
        if (bindingContext.diagnostics.any { it.severity == Severity.ERROR }) return null

        val targetId = TargetId(
            name = compilerConfiguration[CommonConfigurationKeys.MODULE_NAME] ?: module.name.asString(),
            type = "java-production"
        )

        val inlineFunctions = InlineFunctionsRegistry()

        val generationState = GenerationState.Builder(
            project,
            AbiBinaries(reportInlineFunction = { inlineFunctions.add(it) }),
            module,
            bindingContext,
            files.toList(),
            compilerConfiguration
        ).targetId(targetId).build()
        KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)

        val outputDir = compilerConfiguration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)!!
        val outputs = generationState.factory.asList().let { list ->
            val itr = list.iterator()
            Array(list.size) {
                val outputFile = itr.next()
                val file = File(outputDir, outputFile.relativePath)
                AbiOutput(file, outputFile.sourceFiles, outputFile.asByteArray())
            }
        }

        process(outputs, inlineFunctions)
        // private/local/synthetic class removal is temporarily turned off, because the implementation
        // was not correct: it was not taking into account that private/local classes could be used
        // from inline functions
        // todo: implement correct removal
        //removeUnneededClasses(outputs)

        val messageCollector = compilerConfiguration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
        val reportOutputFiles = generationState.configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
        val outputItemsCollector =
            OutputItemsCollector { sourceFiles, outputFile ->
                messageCollector.report(CompilerMessageSeverity.OUTPUT, OutputMessageUtil.formatOutputMessage(sourceFiles, outputFile))
            }.takeIf { reportOutputFiles }
        outputs.forEach { it.flush(outputItemsCollector) }
        return null
    }

    private fun process(outputs: Array<AbiOutput>, inlineFuns: InlineFunctionsRegistry) {
        val localOrSyntheticClasses = HashSet<String>()
        val internalNameToOutputIndex = HashMap<String, Int>()

        for ((i, output) in outputs.withIndex()) {
            if (!output.isClassFile) continue
            val classData = output.classData() ?: continue
            val jvmClassName = JvmClassName.byClassId(classData.classId)
            internalNameToOutputIndex[jvmClassName.internalName] = i

            val header = classData.classHeader
            val internalName = jvmClassName.internalName
            when (header.kind) {
                KotlinClassHeader.Kind.CLASS -> {
                    val (_, classProto) = JvmProtoBufUtil.readClassDataFrom(header.data!!, header.strings!!)
                    val visibility = Flags.VISIBILITY.get(classProto.flags)
                    if (visibility == ProtoBuf.Visibility.LOCAL) {
                        localOrSyntheticClasses.add(internalName)
                    }
                }
                KotlinClassHeader.Kind.SYNTHETIC_CLASS -> {
                    localOrSyntheticClasses.add(internalName)
                }
            }
        }

        val excludedClasses = HashSet<String>()
        val visitedClasses = HashSet<String>()
        val namesToProcess = ArrayDeque<String>()
        namesToProcess.addAll(inlineFuns.owners)
        while (namesToProcess.isNotEmpty()) {
            val internalName = namesToProcess.poll()
            if (!visitedClasses.add(internalName)) continue

            val inlineFunsForClass = inlineFuns.getInlineFuns(internalName)
            if (inlineFunsForClass != null) {
                val index = internalNameToOutputIndex[internalName] ?: continue

                outputs[index].accept(object : ClassVisitor(Opcodes.API_VERSION) {
                    override fun visitMethod(
                        access: Int,
                        name: String,
                        desc: String,
                        signature: String?,
                        exceptions: Array<out String>?
                    ): MethodVisitor? {
                        if (inlineFunsForClass.contains(name = name, desc = desc)) {
                            return object : MethodVisitor(Opcodes.API_VERSION) {
                                override fun visitTypeInsn(opcode: Int, type: String) {
                                    if (opcode == Opcodes.NEW && type in localOrSyntheticClasses) {
                                        excludedClasses.add(type)
                                    }
                                }

                                override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
                                    // todo: comment see usages of handleAnonymousObjectRegeneration
                                    if (opcode == Opcodes.GETSTATIC && owner.contains('$') && owner in localOrSyntheticClasses) {
                                        excludedClasses.add(owner)
                                    }
                                }
                            }
                        }

                        return super.visitMethod(access, name, desc, signature, exceptions)
                    }
                })
            }
        }

        for (i in outputs.indices) {
            outputs[i].transform { internalName, cw ->
                if (internalName !in excludedClasses) {
                    AbiClassTransformingVisitor(inlineFuns = inlineFuns.getInlineFuns(internalName), cv = cw)
                } else null
            }
        }
    }

    private class AbiClassTransformingVisitor(
        private val inlineFuns: MethodsSet?,
        cv: ClassVisitor
    ) : ClassVisitor(Opcodes.API_VERSION, cv) {
        override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            val mv: MethodVisitor? = super.visitMethod(access, name, desc, signature, exceptions)

            if (inlineFuns == null || !inlineFuns.contains(name = name, desc = desc)) {
                return ReplaceWithEmptyMethodVisitor(
                    delegate = mv!!,
                    access = access,
                    name = name,
                    desc = desc,
                    signature = signature,
                    exceptions = exceptions
                )
            }

            return mv
        }
    }

    /**
     * Removes private or local classes from outputs
     */
    // todo: fix usage (see analysisCompleted)
    @Suppress("unused")
    private fun removeUnneededClasses(outputs: Iterable<AbiOutput>) {
        // maps internal names of classes: class -> inner classes
        val innerClasses = HashMap<String, Collection<String>>()
        val internalNameToFile = HashMap<String, File>()

        for (output in outputs) {
            if (!output.isClassFile) continue

            val visitor = InnerClassesCollectingVisitor()
            output.accept(visitor)
            val outputInternalName = visitor.ownInternalName!!
            internalNameToFile[outputInternalName] = output.file
            innerClasses[outputInternalName] = visitor.innerClasses
        }

        // internal names of removed files
        val classesToRemoveQueue = ArrayDeque<String>()
        for (output in outputs) {
            if (!output.isClassFile) continue

            val classData = output.classData() ?: continue
            val header = classData.classHeader
            val isNeededForAbi = when (header.kind) {
                KotlinClassHeader.Kind.CLASS -> {
                    val (_, classProto) = JvmProtoBufUtil.readClassDataFrom(header.data!!, header.strings!!)
                    val visibility = Flags.VISIBILITY.get(classProto.flags)
                    visibility != ProtoBuf.Visibility.PRIVATE && visibility != ProtoBuf.Visibility.LOCAL
                }
                KotlinClassHeader.Kind.SYNTHETIC_CLASS -> false
                else -> true
            }

            if (!isNeededForAbi) {
                val jvmClassName = JvmClassName.byClassId(classData.classId)
                classesToRemoveQueue.add(jvmClassName.internalName)
            }
        }

        // we can remove inner classes of removed classes
        val classesToRemove = HashSet<String>()
        classesToRemove.addAll(classesToRemoveQueue)
        while (classesToRemoveQueue.isNotEmpty()) {
            val classToRemove = classesToRemoveQueue.removeFirst()
            innerClasses[classToRemove]?.forEach {
                if (classesToRemove.add(it)) {
                    classesToRemoveQueue.add(it)
                }
            }
        }

        val classFilesToRemove = classesToRemove.mapTo(HashSet()) { internalNameToFile[it] }
        for (output in outputs) {
            if (!output.isClassFile) continue

            if (output.file in classFilesToRemove) {
                output.delete()
            } else {
                output.transform { _, writer ->
                    FilterInnerClassesVisitor(classesToRemove, Opcodes.API_VERSION, writer)
                }
            }
        }
    }

    private class AbiBinaries(private val reportInlineFunction: (MethodSignatureInfo) -> Unit) : ClassBuilderFactory {
        override fun getClassBuilderMode(): ClassBuilderMode =
            ClassBuilderMode.ABI

        override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder =
            AbiClassBuilder(ClassWriter(0), reportInlineFunction)

        override fun asText(builder: ClassBuilder): String =
            throw UnsupportedOperationException("AbiBinaries generator asked for text")

        override fun asBytes(builder: ClassBuilder): ByteArray {
            val visitor = builder.visitor as ClassWriter
            return visitor.toByteArray()
        }

        override fun close() {}
    }

    private data class ClassData(
        val classId: ClassId,
        val classVersion: Int,
        val classHeader: KotlinClassHeader
    )

    private data class AbiOutput(
        val file: File,
        val sources: List<File>,
        // null bytes means that file should not be written
        private var bytes: ByteArray?
    ) {
        val isClassFile: Boolean = file.path.endsWith(".class")

        fun classData(): ClassData? =
            when {
                bytes == null -> null
                !isClassFile -> null
                else -> FileBasedKotlinClass.create(bytes!!) { classId, classVersion, classHeader, _ ->
                    ClassData(classId, classVersion, classHeader)
                }
            }

        fun delete() {
            bytes = null
        }

        fun transform(fn: (internalName: String, writer: ClassWriter) -> ClassVisitor?) {
            if (!isClassFile) return

            val bytes = bytes ?: return
            val cr = ClassReader(bytes)
            val cw = ClassWriter(0)
            val visitor = fn(cr.className, cw) ?: return
            cr.accept(visitor, 0)
            this.bytes = cw.toByteArray()
        }

        fun accept(visitor: ClassVisitor) {
            val bytes = bytes ?: return
            val cr = ClassReader(bytes)
            cr.accept(visitor, 0)
        }

        fun flush(outputItemsCollector: OutputItemsCollector?) {
            val bytes = bytes ?: return
            FileUtil.writeToFile(file, bytes)
            outputItemsCollector?.add(sources, file)
        }
    }
}
