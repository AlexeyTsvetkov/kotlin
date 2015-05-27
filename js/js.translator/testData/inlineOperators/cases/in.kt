package foo

// CHECK_NOT_CALLED: contains

class X(val x: Int, val y: Int) {
    inline fun contains(n: Int): Boolean = x <= n && n <= y
}

fun box(): String {
    assertEquals(true, 2 in X(1, 3))
    assertEquals(false, 0 in X(1, 3))

    return "OK"
}