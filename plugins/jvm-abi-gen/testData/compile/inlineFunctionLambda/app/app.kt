package app

import lib.*

fun runAppAndReturnOk(): String {
    val a = multiplier(3)(5)
    if (a != 15) error("a is '$a', but is expected to be '15'")

    /*val b = multiplier2(3)(5)(7)
    if (b != 105) error("b is '$b', but is expected to be '105'")*/

    return "OK"
}