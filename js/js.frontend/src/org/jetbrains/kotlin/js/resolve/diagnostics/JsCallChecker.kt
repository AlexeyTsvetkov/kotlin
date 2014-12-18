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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.js.descriptors.PatternBuilder
import org.jetbrains.kotlin.js.descriptors.JS_PATTERN
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetStringTemplateExpression

public class JsCallChecker : CallChecker {

    override fun <F : CallableDescriptor?> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        if (context.isAnnotationContext || !resolvedCall.matchesJsCode()) return

        val expression = resolvedCall.getCall().getCallElement()

        if (expression !is JetCallExpression) return

        checkArgumentIsStringLiteral(expression, context)
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

}

private fun <F : CallableDescriptor?> ResolvedCall<F>.matchesJsCode(): Boolean {
    val descriptor = getResultingDescriptor()

    return when (descriptor) {
        is SimpleFunctionDescriptor -> JS_PATTERN.apply(descriptor)
        else -> false
    }
}

