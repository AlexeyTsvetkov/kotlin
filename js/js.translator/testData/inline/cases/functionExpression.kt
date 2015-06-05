package foo

// CHECK_CONTAINS_NO_CALLS: test

inline fun evaluate<T, R>(x: T, fn: (T)->R): R =
        fn(x)

fun test(x: Int): Int =
        evaluate(x, fun (num: Int): Int { return num * 2 })

fun box(): String {
    assertEquals(6, test(3))

    return "OK"
}