package foo

// CHECK_CONTAINS_NO_CALLS: test

inline fun evaluate<T, R>(x: T, fn: (T)->R): R =
        fn(x)

fun test(x: Int, y: Int): Int =
        evaluate(x, fun (num: Int): Int { return num * y })

fun box(): String {
    assertEquals(9, test(3, 3))

    return "OK"
}