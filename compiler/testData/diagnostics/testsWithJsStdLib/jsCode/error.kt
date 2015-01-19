val code = """
    var s = "hello"
    + );
"""

fun main(args: Array<String>): Unit {
    js("var<!JSCODE_ERROR!> =<!> 10;")

    js("""var<!JSCODE_ERROR!> =<!> 10;""")

    js(<!JSCODE_ERROR!>"var " + " = " + "10;"<!>)

    js(<!JSCODE_ERROR!>code<!>)
}
