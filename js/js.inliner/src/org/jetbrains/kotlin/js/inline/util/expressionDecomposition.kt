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

package org.jetbrains.kotlin.js.inline.util

import com.google.dart.compiler.backend.js.ast.*
import java.util.LinkedList
import kotlin.properties.Delegates

/**
 * Extracts all subexpressions with side effects preceding [expression]
 * so that when expression is inlined, an evaluation order isn't broken.
 *
 * For example:
 *
 *      var a = foo() + inlineBar();
 *
 * If "inlineBar()" consists of multiple statements,
 * it will be inlined before "var a" declaration:
 *
 *      // inlineBar body...
 *      var a = foo() + inlineBarResult;
 *
 * Thus, the evaluation order is broken ("inlineBar" is evaluated before "foo").
 * To preserve the evaluation order, we need to extract all preceding statements.
 * In this case, the resulting code would be:
 *
 *      var fooResult = foo();
 *      // inlineBar body...
 *      var a = fooResult + inlineBarResult;
 *
 * @param expression all subexpressions preceding this will be extracted
 * @param statement first [JsStatement] preceding [expression]
 * @return additional declarations, that need to be inserted before [statement]
 */
public fun extractExpressionsBefore(
        scope: JsScope, expression: JsExpression, statement: JsStatement
): JsVars {
    val predecessors = expression.ancestorsFrom(statement)
    assert(predecessors.isNotEmpty()) { "$statement does not contain $expression" }

    var child = predecessors.pollFirst()
    var parent = predecessors.pollFirst()
    val extractor = ExpressionExtractor(scope)

    while (child != null && parent != null) {
        extractor.extractFromParentBeforeChild(parent!!, child!!)
        child = parent
        parent = predecessors.pollFirst()
    }

    return extractor.additionalVars
}

class ExpressionExtractor(private val scope: JsScope) : JsVisitor() {
    val additionalVars = JsVars()
    private var child: JsExpression by Delegates.notNull()

    fun extractFromParentBeforeChild(parent: JsExpression, child: JsExpression) {
        this.child = child
        accept(parent)
    }

    override fun visitBinaryExpression(x: JsBinaryOperation) {
        if (child === x.getArg1()) return
        assert(child === x.getArg2()) { "$x does not contain $child" }

        val arg1 = x.getArg1()
        if (canHaveSideEffect(arg1)) {
            x.setArg1(newTemporary(arg1))
        }
    }

    override fun visitArrayAccess(x: JsArrayAccess) {
        super.visitArrayAccess(x)
    }

    override fun visitArray(x: JsArrayLiteral) {
        extractChildPredecessors(x.getExpressions())
    }

    override fun visitConditional(x: JsConditional) {
        super.visitConditional(x)
    }

    /**
     * It's ok to call inlineFun in qualifier of [JsInvocation], no extraction needed.
     * If inlineFun call is in arguments, we need to extract preceding arguments,
     * that can have side effects (and qualifier in case it can have side effect).
     *
     * So "noInline1().noInline2(noInline3(), inlineFun())" will transform to:
     *
     *      var tmp$0 = noInline1();
     *      var tmp$1 = noInline3();
     *      // inlineFun body...
     *      tmp$0.noInline2(tmp$1, inlineFunResult);
     */
    override fun visitInvocation(invocation: JsInvocation) {
        val qualifier = invocation.getQualifier()
        if (child === qualifier) return

        extractChildPredecessors(invocation.getArguments())
        if (canHaveSideEffect(qualifier)) {
            invocation.setQualifier(newTemporary(qualifier))
        }
    }

    override fun visitNew(x: JsNew) {
        super.visitNew(x)
    }

    private fun extractChildPredecessors(expressions: MutableList<JsExpression>) {
        for ((i, expr) in expressions.withIndex()) {
            if (expr === child) return

            if (!canHaveSideEffect(expr)) continue

            expressions[i] = newTemporary(expr)
        }

        throw AssertionError("$expressions does not contain $child")
    }

    private fun newTemporary(initExpression: JsExpression): JsNameRef {
        val name = scope.declareTemporary()
        val variable = JsVars.JsVar(name, initExpression)
        additionalVars.getVars().add(0, variable)
        return name.makeRef()
    }
}

/**
 * Gets all ancestor expressions of the receiver from [root].
 * An ancestor is assumed to be:
 *
 * 1. the receiver
 * 2. a node, whose child is an ancestor
 *
 * @return If root contains expression, a list of ancestors is returned.
 * Empty list is returned otherwise.
 *
 * For example, for the AST like this:
 *
 *      root->
 *          expr1->expr2
 *          expr3->receiver
 *
 * the output would be `listOf(receiver, expr3)`.
 */
private fun JsExpression.ancestorsFrom(root: JsStatement): LinkedList<JsExpression> {
    val expression = this
    val predecessors = linkedListOf<JsExpression>()

    val visitor = object : RecursiveJsVisitor() {
        override fun visitElement(node: JsNode) {
            if (expression == node) {
                predecessors.add(expression)
            }

            if (predecessors.isEmpty()) {
                super.visitElement(node)

                if (predecessors.isNotEmpty() && node is JsExpression) {
                    predecessors.add(node)
                }
            }
        }
    }

    visitor.accept(root)
    return predecessors
}
