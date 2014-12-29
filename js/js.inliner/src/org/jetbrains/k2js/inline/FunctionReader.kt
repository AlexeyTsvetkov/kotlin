/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.inline

import com.google.dart.compiler.backend.js.ast.JsFunction
import com.google.dart.compiler.backend.js.ast.JsInvocation
import com.google.dart.compiler.backend.js.ast.metadata.libraryJarFilePath
import java.io.File
import java.net.URL
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import com.google.dart.compiler.backend.js.ast.JsNameRef
import org.jetbrains.k2js.translate.context.TranslationContext
import com.google.dart.compiler.backend.js.ast.JsProgram
import org.jetbrains.k2js.translate.context.Namer.*

public class FunctionReader(private val program: JsProgram) {
    val moduleSourceCache = hashMapOf<String, String>()

    public fun getFunctionDefinition(call: JsInvocation): JsFunction? {
        val path = call.libraryJarFilePath!!
        val source = getJsSource(path)
        val qualifier = call.getQualifier()

        if (qualifier !is JsNameRef) {
            throw AssertionError("Expected JsNameRef")
        }

        val functionIdent = program.getStringLiteral(qualifier.getIdent())
        val prefix = "Kotlin.".length()
        val startTag = getInlineStartTag(functionIdent).toString().substring(prefix)
        val endTag = getInlineEndTag(functionIdent).toString().substring(prefix)

        return null
    }

    private fun getJsSource(path: String): String {
        var source: String? = moduleSourceCache[path]

        if (source == null) {
            source = readJsSource(path)
            moduleSourceCache[path] = source
        }

        return source!!
    }

    private fun readJsSource(path: String): String {
        var reader: BufferedReader? = null

        try{
            val file = File(path)
            val url = URL("jar:file:${file.getAbsolutePath()}!/kotlin.js")
            val input= url.openStream()
            reader = BufferedReader(InputStreamReader(input))
            return reader!!.readText()
        } finally {
            reader?.close()
        }
    }
}