package foo

var counter = 0

fun getIntArray(): IntArray {
    val result = arrayListOf<Int>()

    for (i in 1..++counter) {
        result.add(i)
    }

    return result.copyToArray()
}

fun test(): Int = getIntArray()[sum(counter, -1)]

fun box(): String {
    assertEquals(1, test())
    assertEquals(2, test())
    assertEquals(3, test())

    return "OK"
}