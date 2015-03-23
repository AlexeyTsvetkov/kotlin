package foo

// CHECK_NOT_CALLED: getSafe

inline fun IntArray.getSafe(index: Int, defaultValue: Int): Int {
    if (index < 0 || index >= size()) return defaultValue

    return get(index)
}

fun IntArray.sumUntil(stopValue: Int): Int {
    var sum = 0
    var i = 0

    while (getSafe(i, stopValue) != stopValue) {
        if (i > size()) throw Exception("Timeout!")

        sum += get(i)
        i++
    }

    return sum
}

fun box(): String {
    assertEquals(3, intArray(1, 1, 1).sumUntil(-1))
    assertEquals(1, intArray(1, -1, 1).sumUntil(-1))

    return "OK"
}