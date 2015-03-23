package foo

// CHECK_NOT_CALLED: sum

var counter = 0

fun incCounterIfNull(x: Int?): Int? {
    if (x == null) counter++

    return x
}

fun test(x: Int?): Int = incCounterIfNull(x) ?: sum(counter, counter)

fun box(): String {
    assertEquals(1, test(1))
    assertEquals(2, test(null))
    assertEquals(4, test(null))
    assertEquals(2, test(2))

    return "OK"
}