fun main(args: Array<String>): Unit {
    js("var<!JSCODE_ERROR!> =<!> 10;")

    js("""var<!JSCODE_ERROR!> =<!> 10;""")
}
