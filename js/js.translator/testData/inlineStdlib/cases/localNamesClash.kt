package foo

// CHECK_CONTAINS_NO_CALLS: sumX2

fun sumX2(x: Int, y: Int): Int =
        with (x + x) {
            val xx = this

            with (y + y) {
                xx + this
            }
        }

fun box(): String {
    assertEquals(10, sumX2(2, 3))
    assertEquals(18, sumX2(4, 5))

    return "OK"
}