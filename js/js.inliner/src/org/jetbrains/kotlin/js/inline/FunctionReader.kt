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

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.utils.PathUtil

import com.intellij.util.containers.SLRUCache
import java.io.*
import java.net.URL

public class FunctionReader(private val context: TranslationContext) {
    private val sourceFileCache = object : SLRUCache<String, String>(10, 10) {
        override fun createValue(path: String): String =
                requireNotNull(readSourceFile(path), "Could not read file: $path")
    }

    private fun readSourceFile(path: String): String? {
        var reader: BufferedReader? = null

        try{
            val file = File(path)
            val url = URL("jar:file:${file.getAbsolutePath()}!/kotlin.js")
            val input= url.openStream()
            reader = BufferedReader(InputStreamReader(input))
            return reader?.readText()
        } finally {
            reader?.close()
        }
    }
}