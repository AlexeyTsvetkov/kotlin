package foo

// CHECK_NOT_CALLED: plus

class X(val x: Int) {
    inline fun plus(): Int = if (x < 0) -x else x
}

fun box(): String {
    assertEquals(1, +X(-1))
    assertEquals(1, +X(1))

    return "OK"
}