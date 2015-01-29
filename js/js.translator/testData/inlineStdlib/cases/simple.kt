package foo

// CHECK_CONTAINS_NO_CALLS: count

var counter = 0

fun count(a: Int) {
    a.times {
        counter += 1
    }
}

fun box(): String {
    assertEquals(0, counter)
    count(5)
    assertEquals(5, counter)

    return "OK"
}