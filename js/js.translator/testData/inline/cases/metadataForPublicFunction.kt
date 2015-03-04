package foo

// CHECK_CONTAINS_NO_CALLS: test1
// CHECK_CONTAINS_NO_CALLS: test2
// CHECK_CONTAINS_NO_CALLS: test3
// CHECK_HAS_INLINE_METADATA: apply_hiyix$
// CHECK_HAS_INLINE_METADATA: applyM_hiyix$
// CHECK_HAS_NO_INLINE_METADATA: applyN

inline
public fun apply<T>(arg: T, func: (T)->T): T = func(arg)

public class M {
    inline
    public fun applyM<T>(arg: T, func: (T)->T): T = func(arg)
}

private class N {
    inline
    public fun applyN<T>(arg: T, func: (T)->T): T = func(arg)
}

fun test1(x: Int, y: Int): Int = apply(x) { it * y }

fun test2(m: M, x: Int, y: Int): Int = m.applyM(x) { it * y }

fun test3(n: N, x: Int, y: Int): Int = n.applyN(x) { it * y }

fun box(): String {
    assertEquals(6, test1(2, 3))
    assertEquals(20, test1(5, 4))

    assertEquals(6, test2(M(), 2, 3))
    assertEquals(20, test2(M(), 5, 4))

    assertEquals(6, test3(N(), 2, 3))
    assertEquals(20, test3(N(), 5, 4))

    return "OK"
}