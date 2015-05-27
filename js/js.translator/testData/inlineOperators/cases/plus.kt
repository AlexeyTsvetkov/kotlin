package foo

// CHECK_NOT_CALLED: plus

class X(val x: Int) {
    inline fun plus(y: Int): Int = x + y
}

fun box(): String {
    assertEquals(3, X(1) + 2)

    return "OK"
}