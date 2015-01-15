val a = "a"

fun test() {
    val b = "b+b +"

    js(<!JSCODE_ARGUMENT_SHOULD_BE_LITERAL!>a<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_LITERAL!>b<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_LITERAL!>"$a"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_LITERAL!>"$b;"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_LITERAL!>"$b bb"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_LITERAL!>a + a<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_LITERAL!>"a" + "a"<!>)

    js("ccc")
}