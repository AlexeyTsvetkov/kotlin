package foo

// CHECK_NOT_CALLED: sum

var counter = 0

fun incAndGet(): Int = ++counter

class Sum(x: Int, y: Int) {
    val value = x + y
}

fun test(): Int = Sum(incAndGet(), sum(counter, counter)).value

fun box(): String {
    assertEquals(3, test())
    assertEquals(6, test())

    return "OK"
}