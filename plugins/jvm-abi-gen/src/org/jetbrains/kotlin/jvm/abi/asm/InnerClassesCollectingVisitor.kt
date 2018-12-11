package org.jetbrains.kotlin.jvm.abi.asm

import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

internal class InnerClassesCollectingVisitor : ClassVisitor(AbiExtensionAsmApiVersion) {
    lateinit var ownInternalName: String
        private set

    private val myInnerClasses = arrayListOf<String>()
    val innerClasses: List<String>
        get() = myInnerClasses

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        ownInternalName = name
    }

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        super.visitInnerClass(name, outerName, innerName, access)
        myInnerClasses.add(name)
    }

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        val mv = super.visitMethod(access, name, desc, signature, exceptions)
        return object : MethodVisitor(AbiExtensionAsmApiVersion, mv) {
            override fun visitTypeInsn(opcode: Int, type: String?) {
                super.visitTypeInsn(opcode, type)
            }

            override fun visitLdcInsn(cst: Any?) {
                Opcodes.GETSTATIC
                super.visitLdcInsn(cst)
            }

            override fun l(opcode: Int, owner: String?, name: String?, desc: String?) {
                super.visitFieldInsn(opcode, owner, name, desc)
            }
        }
    }
}
