package foo

fun assertEquals<T>(expected: T, actual: T) {
    if (expected != actual) throw Exception("expected -- $expected, actual -- $actual")
}

fun assertNotEquals<T>(unexpected: T, actual: T) {
    if (actual == unexpected) throw Exception("unexpected -- $unexpected")
}

fun assertArrayEquals<T>(expected: Array<T>, actual: Array<T>) {
    val expectedSize = expected.size
    val actualSize = actual.size

    if (expectedSize != actualSize) {
        throw Exception("expected size -- $expectedSize, actual size -- $actualSize")
    }

    for (i in 0..expectedSize) {
        val expectedIth = expected[i]
        val actualIth = actual[i]

        if (expected[i] != actual[i]) {
            throw Exception("expected[$i] -- $expectedIth, actual[$i] -- $actualIth")
        }
    }
}

fun assertEquals<T>(expected: T, actual: T, at: String) {
    if (expected != actual) throw Exception("Error at $at: expected -- $expected, actual -- $actual")
}