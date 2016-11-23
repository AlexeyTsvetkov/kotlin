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

package org.jetbrains.kotlin.incremental.components

import java.io.*
import java.util.*

internal class SourceAnnotationsRegistry(private val file: File) : SourceRetentionAnnotationHandler {
    private val mutableAnnotations: MutableSet<String> = HashSet()
    val annotations: Set<String>
            get() = mutableAnnotations

    init {
        file.delete()
    }

    override fun register(internalName: String) {
        mutableAnnotations.add(internalName)
    }

    override fun flush() {
        file.parentFile.mkdirs()
        file.createNewFile()

        ObjectOutputStream(BufferedOutputStream(file.outputStream())).use { out ->
            out.writeInt(mutableAnnotations.size)
            mutableAnnotations.forEach { out.writeUTF(it) }
        }
    }
}
