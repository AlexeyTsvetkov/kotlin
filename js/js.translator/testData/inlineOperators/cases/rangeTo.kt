package foo

// CHECK_NOT_CALLED: rangeTo

class X(val x: Int) {
    inline fun rangeTo(y: Int): IntRange = x..y
}

fun box(): String {
    assertEquals(1..5, X(1)..5)

    return "OK"
}