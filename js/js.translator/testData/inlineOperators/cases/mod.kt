package foo

// CHECK_NOT_CALLED: mod

class X(val x: Int) {
    inline fun mod(y: Int): Int = x % y
}

fun box(): String {
    assertEquals(1, X(3) % 2)

    return "OK"
}