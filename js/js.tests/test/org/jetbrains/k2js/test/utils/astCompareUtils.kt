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

package org.jetbrains.k2js.test.utils

import com.google.dart.compiler.backend.js.ast.*
import kotlin.reflect.jvm.*
import kotlin.reflect.*

public fun JsNode.match(matcher: JsNode.() -> AstMatcher) {
    matcher().assertMatches()
}

public fun JsNode.block(init: StatementsMatcher.() -> Unit): AstMatcher =
        InstanceMatcher(this, javaClass<JsBlock>()) {
            val matcher = StatementsMatcher(getStatements())
            matcher.init()
            matcher
        }

public fun JsStatement.statement(matcher: JsExpression.() -> AstMatcher): AstMatcher =
        InstanceMatcher(this, javaClass<JsExpressionStatement>()) {
            getExpression().matcher()
        }

public fun JsStatement.ifBlock(init: IfMatcher.() -> Unit): AstMatcher =
        InstanceMatcher(this, javaClass<JsIf>()) {
            val ifNode: JsIf = this
            val matcher = IfMatcher(ifNode)
            matcher.init()
            matcher
        }

public fun <T : JsNode> T.any(): AstMatcher = WildcardMatcher

trait AstMatcher {
    fun assertMatches(): Unit
}

object WildcardMatcher : AstMatcher {
    override fun assertMatches(): Unit {}
}

class InstanceMatcher<T : JsNode>(
        val node: JsNode,
        val type: Class<T>,
        val typeSpecificMatcher: T.() -> AstMatcher
) : AstMatcher {
    override fun assertMatches() {
        assert (type.isInstance(node)) {
            "Type mismatch: expected -- $type, actual -- ${node.javaClass} for node: $node"
        }

        type.cast(node).typeSpecificMatcher().assertMatches()
    }
}

open class NodesMatcher<T : JsNode>(val nodes: List<T>) : AstMatcher {
    var exactSize: Boolean = false
    private val matchers = arrayListOf<T.() -> AstMatcher>()

    fun any() {
        add { WildcardMatcher }
    }

    protected fun add(matcher: T.() -> AstMatcher) {
        matchers.add(matcher)
    }

    override fun assertMatches() {
        assert (!exactSize || nodes.size == matchers.size) {
            // TODO: better message
            "Nodes size is not equal to matchers size"
        }

        for ((node, matcher) in nodes zip matchers) {
            node.matcher().assertMatches()
        }
    }
}

class StatementsMatcher(nodes: List<JsStatement>) : NodesMatcher<JsStatement>(nodes) {
    fun ifBlock(init: IfMatcher.() -> Unit) = add { ifBlock { init() } }
    fun statement(matcher: JsExpression.() -> AstMatcher) = add { statement { matcher() } }
}

class IfMatcher(val ifNode: JsIf) : AstMatcher {
    private var testMatcher: JsExpression.() -> AstMatcher = { any() }
    private var thenMatcher: JsStatement.() -> AstMatcher = { any() }
    private var elseMatcher: KExtensionFunction0<JsStatement, AstMatcher>? = null

    fun test(matcher: JsExpression.() -> AstMatcher) {
        testMatcher = matcher
    }

    fun then(matcher: JsStatement.() -> AstMatcher) {
        thenMatcher = matcher
    }

    fun otherwise(matcher: KExtensionFunction0<JsStatement, AstMatcher>) {
        elseMatcher = matcher
    }

    override fun assertMatches() {
        ifNode.getIfExpression().testMatcher()
        ifNode.getThenStatement().thenMatcher()

        if (elseMatcher != null) {
            val matcher = elseMatcher!!
            val elseStatement = ifNode.getElseStatement()
            assert (elseStatement != null) {
                "JsIf is expected to have else statement, but found none:\n${ifNode}"
            }

            elseStatement.matcher()
        }
    }
}

fun box() {
    val node = JsBlock(JsIf(JsLiteral.TRUE, JsBlock()))

    node match {
        block {
            any()

            ifBlock {
                then { any() }
                otherwise { any() }
            }
        }
    }
}