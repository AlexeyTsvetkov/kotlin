/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.annotation

import java.io.File
import java.io.StringReader
import org.jetbrains.kotlin.annotation.CompactNotationType as Notation

// When compiling incrementally, we generate annotation file only for just compiled classes.
// But annotation processor needs all annotations.
// So the workaround is to:
// 1. save old annotations somewhere,
// 2. remove records about deleted and recompiled classes,
// 3. add records about new and recompiled classes (from new annotations file).
class MutableKotlinAnnotationProvider : KotlinAnnotationProvider(StringReader("")) {
    fun addAnnotationsFrom(file: File) {
        val reader = file.bufferedReader()

        try {
            readAnnotations(reader)
        }
        finally {
            reader.close()
        }
    }

    fun removeClasses(classesFqNames: Set<String>) {
        kotlinClassesInternal.removeAll(classesFqNames)

        for ((annotation, elements) in annotatedKotlinElementsInternal) {
            elements.removeAll { it.classFqName in classesFqNames }
        }
    }
}