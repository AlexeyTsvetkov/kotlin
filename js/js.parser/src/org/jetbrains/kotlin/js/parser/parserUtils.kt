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

package org.jetbrains.kotlin.js.parser

import com.google.dart.compiler.common.*
import com.google.dart.compiler.backend.js.ast.*
import com.google.gwt.dev.js.*
import com.google.gwt.dev.js.rhino.*

import com.intellij.util.*
import java.io.*
import java.util.*

private val FAKE_SOURCE_INFO = SourceInfoImpl(null, 0, 0, 0, 0)

throws(javaClass<IOException>())
public fun parse(
        code: String,
        errorReporter: ErrorReporter,
        insideFunction: Boolean
): List<JsStatement> {
    val ts = TokenStream(StringReader(code, 0), "<parser>", FAKE_SOURCE_INFO.getLine())
    val parser = Parser(IRFactory(ts), insideFunction)
    Context.enter().setErrorReporter(errorReporter)

    val statements = arrayListOf<JsStatement>()

    try {
        val topNode = parser.parse(ts) as Node
        JsAstMapper(RootScope()).mapStatements(statements, topNode)
    } finally {
        Context.exit()
    }

    return statements
}

throws(javaClass<IOException>())
public fun parseFunction(
        code: String,
        offset: Long,
        errorReporter: ErrorReporter
): JsFunction {
    val ts = TokenStream(StringReader(code, offset), "<parser>", FAKE_SOURCE_INFO.getLine())
    val parser = Parser(IRFactory(ts), false)
    Context.enter().setErrorReporter(errorReporter)

    var functionsStarted = 0
    parser.addObserver { (observable, arg) ->
        when (arg) {
            is ParserEvents.OnFunctionParsingStart -> {
                functionsStarted++
            }
            is ParserEvents.OnFunctionParsingEnd -> {
                functionsStarted--
                if (functionsStarted == 0) {
                    arg.tokenStream.ungetToken(TokenStream.EOF)
                }
            }
        }
    }

    try {
        val topNode = parser.primaryExpr(ts) as Node
        return JsAstMapper(RootScope()).map(topNode) as JsFunction
    } finally {
        Context.exit()
    }
}

private fun StringReader(string: String, offset: Long): Reader {
    val reader = StringReader(string)
    reader.skip(offset)
    return reader
}

private fun RootScope(): JsScope {
    return JsRootScope(JsProgram("<parser>"))
}