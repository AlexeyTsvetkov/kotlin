package foo

// CHECK_NOT_CALLED: minus

class X(val x: Int) {
    inline fun minus(): Int = -x
}

fun box(): String {
    assertEquals(1, -X(-1))
    assertEquals(-1, -X(1))

    return "OK"
}