package foo

// CHECK_NOT_CALLED: sum

var counter = 0

fun incAndGet(): Int = ++counter

fun test(): Int = array(incAndGet(), sum(counter, counter)).last()

fun box(): String {
    assertEquals(2, test())
    assertEquals(4, test())

    return "OK"
}