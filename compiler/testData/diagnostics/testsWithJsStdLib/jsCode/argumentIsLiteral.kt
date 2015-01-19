val a = "1"

fun test() {
    val b = "b"

    js(a)
    js(b)
    js("$a")
    js("${1}")
    js("$b;")
    js("${b}bb")
    js(a + a)
    js("a" + "a")
    js("ccc")

    var notConst = ""

    for (i in 1..10) {
        notConst += "$i;"
    }

    js(<!JSCODE_ARGUMENT_SHOULD_BE_LITERAL!>notConst<!>)
}