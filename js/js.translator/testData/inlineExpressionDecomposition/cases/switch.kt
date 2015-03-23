package foo

// CHECK_NOT_CALLED: match
// CHECK_NOT_CALLED: test$f
// CHECK_NOT_CALLED: test$f_1
// CHECK_NOT_CALLED: test$f_2

var counter = 0

inline fun match<T>(value: ()->T, caseValue1: ()->T, caseValue2: ()->T): Int =
    js("""
    switch (value()) {
        case caseValue1(): return 1;
        case caseValue2(): return 2;
        default: return 0;
    }
    """)

fun test(x: Int, y: Int, z: Int): Int =
        match({ counter++; sum(x, x) }, { counter++; sum(y, y) }, { counter++; sum(z, z) })

fun box(): String {
    assertEquals(1, test(1, 1, 2))
    assertEquals(2, counter)

    assertEquals(2, test(1, 2, 1))
    assertEquals(5, counter)

    assertEquals(0, test(1, 2, 3))
    assertEquals(8, counter)

    return "OK"
}