package foo

// CHECK_CONTAINS_NO_CALLS: multiply

inline
public fun apply<T>(arg: T, func: (T)->T): T = func(arg)

fun multiply(x: Int, y: Int): Int = apply(x) { it * y }

fun box(): String {
    assertEquals(6, multiply(2, 3))
    assertEquals(20, multiply(5, 4))

    return "OK"
}