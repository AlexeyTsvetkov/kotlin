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

package org.jetbrains.kotlin.resolve.diagnostics

import com.google.gwt.dev.js.AbortParsingException
import com.google.gwt.dev.js.rhino.*
import com.google.gwt.dev.js.rhino.Utils.*
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.ParametrizedDiagnostic
import org.jetbrains.kotlin.js.descriptors.JS_PATTERN
import org.jetbrains.kotlin.js.descriptors.PatternBuilder
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetStringTemplateExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

import com.intellij.openapi.util.TextRange
import java.io.StringReader

public class JsCallChecker : CallChecker {

    override fun <F : CallableDescriptor?> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        if (context.isAnnotationContext || !resolvedCall.matchesJsCode()) return

        val expression = resolvedCall.getCall().getCallElement()
        if (expression !is JetCallExpression) return

        val codeArgument = checkArgumentIsStringLiteral(expression, context)
        if (codeArgument == null) return

        checkSyntax(codeArgument, context)
    }

    fun checkArgumentIsStringLiteral(
            call: JetCallExpression,
            context: BasicCallResolutionContext
    ): JetStringTemplateExpression? {
        val arguments = call.getValueArgumentList()?.getArguments()
        val argument = arguments?.first?.getArgumentExpression()

        if (argument !is JetStringTemplateExpression) {
            context.trace.report(ErrorsJs.JSCODE_ARGUMENT_SHOULD_BE_LITERAL.on(call))
            return null
        }

        return argument
    }

    fun checkSyntax(jsCodeExpression: JetStringTemplateExpression, context: BasicCallResolutionContext) {
        val bindingContext = context.trace.getBindingContext()
        val codeConstant = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, jsCodeExpression);
        val code = codeConstant.getValue() as String
        val reader = StringReader(code)

        val errorReporter = JsCodeErrorReporter(jsCodeExpression, code, context.trace)
        Context.enter().setErrorReporter(errorReporter)

        try {
            val ts = TokenStream(reader, "js", 0)
            val parser = Parser(IRFactory(ts), /* insideFunction = */ true)
            parser.parse(ts)
        } catch (e: AbortParsingException) {
            // ignore
        } finally {
            Context.exit()
        }
    }

}

private fun <F : CallableDescriptor?> ResolvedCall<F>.matchesJsCode(): Boolean {
    val descriptor = getResultingDescriptor()

    return when (descriptor) {
        is SimpleFunctionDescriptor -> JS_PATTERN.apply(descriptor)
        else -> false
    }
}

private class JsCodeErrorReporter(
        private val jsCodeExpression: JetStringTemplateExpression,
        private val code: String,
        private val trace: DiagnosticSink
) : ErrorReporter {

    override fun error(message: String, sourceName: String, line: Int, lineSource: String, lineOffset: Int) {
        val diagnostic = getDiagnostic(ErrorsJs.JSCODE_ERROR, message, line, lineOffset)
        trace.report(diagnostic)
        throw AbortParsingException()
    }

    override fun warning(message: String, sourceName: String, line: Int, lineSource: String, lineOffset: Int) {
        val diagnostic = getDiagnostic(ErrorsJs.JSCODE_WARNING, message, line, lineOffset)
        trace.report(diagnostic)
    }

    private fun getDiagnostic(
            diagnosticFactory: DiagnosticFactory2<JetExpression, String, List<TextRange>>,
            message: String,
            line: Int,
            lineOffset: Int
    ): ParametrizedDiagnostic<JetExpression> {
        var offset = jsCodeExpression.getTextOffset() + offsetFromStart(code, line, lineOffset)

        assert(jsCodeExpression is JetStringTemplateExpression, "js argument is expected to be compile-time string literal")
        val quotesLength = jsCodeExpression.getFirstChild().getTextLength()
        offset += quotesLength

        val textRange = TextRange(offset, offset + 1)
        return diagnosticFactory.on(jsCodeExpression, message, listOf(textRange))
    }

    /**
     * Calculates an offset from the start of a text for a position,
     * defined by line and offset in that line.
     */
    private fun offsetFromStart(text: String, line: Int, offset: Int): Int {
        var i = 0
        var lineCount = 0
        var offsetInLine = 0

        while (i < text.length()) {
            val c = text.charAt(i)

            if (lineCount == line && offsetInLine == offset) {
                return i
            }

            if (isEndOfLine(c.toInt())) {
                offsetInLine = 0
                lineCount++
                assert(lineCount <= line)
            }

            i++
            offsetInLine++
        }

        return text.length()
    }
}

