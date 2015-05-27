package foo

// CHECK_NOT_CALLED: div

class X(val x: Int) {
    inline fun div(y: Int): Int = x / y
}

fun box(): String {
    assertEquals(3, X(6) / 2)

    return "OK"
}