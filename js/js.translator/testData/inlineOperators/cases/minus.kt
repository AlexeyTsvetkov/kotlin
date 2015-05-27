package foo

// CHECK_NOT_CALLED: minus

class X(val x: Int) {
    inline fun minus(y: Int): Int = x - y
}

fun box(): String {
    assertEquals(1, X(3) - 2)

    return "OK"
}