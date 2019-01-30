package app

import lib.*

fun runAppAndReturnOk(): String {
    println(foo(10)())
    // appender("world").apply("hello ")
    return "OK"
}
