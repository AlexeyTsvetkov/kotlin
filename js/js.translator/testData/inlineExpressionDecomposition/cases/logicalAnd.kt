package foo

// CHECK_NOT_CALLED: isEvenUnsafe

fun isEven(n: Int): Boolean = n % 2 == 0

inline fun isEvenUnsafe(n: Int): Boolean {
    if (n % 2 == 0) {
        return true
    }

    throw Exception("Danger!")
}

fun test(n: Int): Boolean = isEven(n) && isEvenUnsafe(n)

fun box(): String {
    assertEquals(true, test(2))
    assertEquals(false, test(1))

    return "OK"
}