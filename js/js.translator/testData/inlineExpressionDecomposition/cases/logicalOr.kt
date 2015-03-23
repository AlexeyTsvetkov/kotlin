package foo

// CHECK_NOT_CALLED: incIfEven

var counter = 0

inline fun incIfEven(): Boolean {
    if (counter % 2 == 0) {
        counter++
        return true
    }

    return false
}

fun incIfEvenNoinline(): Boolean = incIfEven()

fun test(): Boolean = incIfEvenNoinline() || incIfEven()

fun box(): String {
    assertEquals(true, test())
    assertEquals(1, counter)
    assertEquals(false, test())
    assertEquals(2, counter)

    return "OK"
}