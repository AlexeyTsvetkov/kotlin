package app

import lib.*

fun runAppAndReturnOk(): String {
    val a = sum(1, 3)
    if (a != 4) error("a is '$a', but is expected to be '4'")

    return "OK"
}