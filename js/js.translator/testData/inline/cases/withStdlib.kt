package foo

// CHECK_NOT_CALLED: filter_azvtw4$

fun List<Int>.filterEven(): List<Int> = filter { it % 2 == 0 }

fun box(): String {
    assertEquals(listOf(2, 4), listOf(1,2,3,4).filterEven())
    return "OK"
}