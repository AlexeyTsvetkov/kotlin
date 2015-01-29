package foo

// CHECK_CONTAINS_NO_CALLS: times

fun times(a: Int, b: Int): Int {
    var c = 0

    b.times {
        c += a
    }

    return c
}

fun box(): String {
    assertEquals(6, times(2, 3))
    assertEquals(6, times(3, 2))
    assertEquals(20, times(4, 5))

    return "OK"
}