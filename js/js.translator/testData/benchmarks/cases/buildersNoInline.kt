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

package foo

native fun <T> Array<T>.push(element: T): Unit = noImpl

trait Element {
    fun render(builder: StringBuilder, indent: String)

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

class TextElement(val text: String): Element {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$text")
    }
}

abstract class Tag(val name: String): Element {
    val children = array<Element>()

    noinline protected fun initTag<T: Element>(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.push(tag)
        return tag
    }

    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent<$name>")
        for (c in children) {
            c.render(builder, indent)
        }
        builder.append("$indent</$name>")
    }
}

abstract class TagWithText(name: String): Tag(name) {
    noinline fun String.plus() {
        children.push(TextElement(this))
    }
}

class HTML(): TagWithText("html") {
    noinline fun head(init: Head.() -> Unit) = initTag(Head(), init)
    noinline fun body(init: Body.() -> Unit) = initTag(Body(), init)
}

class Head(): TagWithText("head") {
    noinline fun title(init: Title.() -> Unit) = initTag(Title(), init)
}

class Title(): TagWithText("title")

abstract class BodyTag(name: String): TagWithText(name) {
    noinline fun b(init: B.() -> Unit) = initTag(B(), init)
    noinline fun p(init: P.() -> Unit) = initTag(P(), init)
    noinline fun h1(init: H1.() -> Unit) = initTag(H1(), init)
}

class Body(): BodyTag("body")

class B(): BodyTag("b")
class P(): BodyTag("p")
class H1(): BodyTag("h1")

noinline fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}

fun box(): String {
    val result =
            html {
                head {
                    title { +"Head Title" }
                }
                body {
                    h1 { +"Body H1" }
                    p {
                        b { h1 { +"Body P H1" } }
                    }
                }
            }

    //assertEquals("<html><head><title>Head Title</title></head><body><h1>Body H1</h1><p><b><h1>Body P H1</h1></b></p></body></html>",
    //             "$result")
    return "OK"
}