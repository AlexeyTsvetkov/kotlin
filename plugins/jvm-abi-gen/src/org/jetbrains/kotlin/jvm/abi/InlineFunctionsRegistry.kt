/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.jvm.abi.asm.MethodSignatureInfo
import java.util.HashMap
import java.util.HashSet

internal class InlineFunctionsRegistry {
    private val inlineFuns = HashMap<String, MutableMethodsSet>()

    fun add(method: MethodSignatureInfo) {
        inlineFuns.getOrPut(method.owner) { MutableMethodsSet() }.add(method)
    }

    fun getInlineFuns(owner: String): MethodsSet? =
        inlineFuns[owner]

    val owners: Collection<String> =
        inlineFuns.keys
}

internal interface MethodsSet {
    fun contains(name: String, desc: String): Boolean
}

internal class MutableMethodsSet : MethodsSet {
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