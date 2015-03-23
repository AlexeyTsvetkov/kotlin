package foo

// CHECK_NOT_CALLED: sum

var counter = 0

fun incAndGet(): Int = ++counter

fun sumNoinline(x: Int, y: Int): Int = x + y

fun test(): Int = sumNoinline(incAndGet(), sum(counter, counter))

fun box(): String {
    assertEquals(3, test())
    assertEquals(6, test())

    return "OK"
}