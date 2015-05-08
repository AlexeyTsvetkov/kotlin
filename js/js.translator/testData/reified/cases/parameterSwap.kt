package foo

class T
class R
class L

inline fun test<reified T, reified R>(x: Any): String = test1<R, T>(x)

inline fun test1<reified R, reified T>(x: Any): String =
    when (x) {
        is R -> "R"
        is T -> "T"
        else -> "Unknown"
    }

fun box(): String {
    assertEquals("T", test<T, R>(T()), "test<T, R>(T())")
    assertEquals("R", test<T, R>(R()), "test<T, R>(R())")
    assertEquals("Unknown", test<T, R>(L()), "test<T, R>(L())")

    return "OK"
}