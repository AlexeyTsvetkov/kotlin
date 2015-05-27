package foo

// CHECK_NOT_CALLED: times

class X(val x: Int) {
    inline fun times(y: Int): Int = x * y
}

fun box(): String {
    assertEquals(6, X(2) * 3)

    return "OK"
}