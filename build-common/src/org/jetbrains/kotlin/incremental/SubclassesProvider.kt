/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.name.FqName
import java.util.*

interface SubclassesProvider {
    fun subclasses(fqName: FqName): Collection<FqName>
}

class SubclassesProviderImpl(private val caches: Iterable<IncrementalCacheCommon>) : SubclassesProvider {
    override fun subclasses(fqName: FqName): Collection<FqName> {
        val types = LinkedList(listOf(fqName))
        val subtypes = hashSetOf<FqName>()

        while (types.isNotEmpty()) {
            val unprocessedType = types.pollFirst()

            caches.asSequence()
                .flatMap { it.getSubtypesOf(unprocessedType) }
                .filter { it !in subtypes }
                .forEach { types.addLast(it) }

            subtypes.add(unprocessedType)
        }

        subtypes.remove(fqName)
        return subtypes
    }
}

class TracingSubclassesProvider(private val delegate: SubclassesProvider) : SubclassesProvider {
    private val tracedSubclassesMap = HashMap<FqName, MutableSet<FqName>>()
    val tracedSubclasses: Map<FqName, Set<FqName>>
        get() = tracedSubclassesMap

    override fun subclasses(fqName: FqName): Collection<FqName> =
        delegate.subclasses(fqName).also { subtypes ->
            tracedSubclassesMap.getOrPut(fqName) { HashSet() }.addAll(subtypes)
        }
}