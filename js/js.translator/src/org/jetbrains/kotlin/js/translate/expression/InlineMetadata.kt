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

package org.jetbrains.kotlin.js.translate.expression

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.*

import kotlin.platform.platformStatic

private enum class Property {
    START_TAG
    FUNCTION
    MODULE_NAME
    END_TAG
}

public class InlineMetadata private (private val properties: Map<Property, JsExpression>) {
    class object {
        platformStatic
        fun compose(function: JsFunction, descriptor: CallableDescriptor, context: TranslationContext): InlineMetadata {
            val program = function.getScope().getProgram()
            val startTag = program.getStringLiteral(Namer.getInlineStartTag(descriptor))
            val endTag = program.getStringLiteral(Namer.getInlineEndTag(descriptor))
            val moduleName = program.getStringLiteral(context.getConfig().getModuleId())

            val properties = mapOf(Property.START_TAG to startTag,
                                   Property.FUNCTION to function,
                                   Property.MODULE_NAME to moduleName,
                                   Property.END_TAG to endTag)

            return InlineMetadata(properties)
        }

        /**
         * Reads metadata from expression.
         *
         * To read metadata from source one needs to:
         * 1. find index of startTag and endTag in source;
         * 2. parse substring between startTagIndex - 1 (for opening quote)
         *    and endTagIndex + endTag.length() + 1 (for closing quote)
         * 3. call InlineMetadata#decompose on resulting expression
         *
         * @see Namer#getInlineStartTag
         * @see Namer#getInlineEndTag
         * @see com.google.gwt.dev.js.JsParser
         */
        platformStatic
        fun decompose(expression: JsExpression?): InlineMetadata? =
            when (expression) {
                is JsBinaryOperation -> decomposeCommaExpression(expression)
                is JsInvocation -> decomposeCreateFunctionCall(expression)
                else -> null
            }

        private fun decomposeCreateFunctionCall(call: JsInvocation): InlineMetadata? {
            if (Namer.CREATE_INLINE_FUNCTION != call.getQualifier()) return null

            return decomposePropertiesList(call.getArguments())
        }

        private fun decomposeCommaExpression(expression: JsExpression): InlineMetadata? {
            val properties = arrayListOf<JsExpression>()
            var decomposable: JsExpression? = expression

            while (decomposable is JsExpression) {
                val binOp = decomposable as? JsBinaryOperation

                if (JsBinaryOperator.COMMA == binOp?.getOperator()) {
                    properties.add(binOp?.getArg2())
                    decomposable = binOp?.getArg1()
                } else {
                    properties.add(decomposable)
                    break
                }
            }

            return decomposePropertiesList(properties.reverse())
        }

        private fun decomposePropertiesList(properties: List<JsExpression>): InlineMetadata? {
            val types = Property.values()

            if (properties.size() != types.size()) return null

            return InlineMetadata(types.zip(properties).toMap())
        }
    }

    public val function: JsFunction
        get() = properties[Property.FUNCTION] as JsFunction

    public val moduleName: String
        get() = (properties[Property.MODULE_NAME] as JsStringLiteral).getValue()

    public val functionWithMetadata: JsExpression
        get() {
            val propertiesList = arrayListOf<JsExpression>()

            for (property in Property.values()) {
                val value = properties[property]
                assert(properties[property] != null) { "Inline metadata property $property has not been set" }
                propertiesList.add(value)
            }

            return JsInvocation(Namer.CREATE_INLINE_FUNCTION, propertiesList)
        }
}