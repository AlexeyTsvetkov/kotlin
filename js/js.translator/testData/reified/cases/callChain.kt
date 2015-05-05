package foo

// CHECK_NOT_CALLED: test
// CHECK_NOT_CALLED: test1

class A

class B

class C

inline fun test<reified L, reified R>(x: Any): String = test1<R, L>(x)

inline fun test1<reified R, reified L>(x: Any): String =
        if (x is R) {
            "R"
        }
        else if (x is L) {
            "L"
        }
        else {
            "Unknown"
        }

fun box(): String {
    val a: Any = A()
    val b: Any = B()
    val c: Any = C()

    assertEquals("L", test<A, B>(a))
    assertEquals("R", test<A, B>(b))
    assertEquals("Unknown", test<A, B>(c))

    return "OK"
}