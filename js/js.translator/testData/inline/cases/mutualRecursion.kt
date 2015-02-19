package foo

inline fun f() { g() }

inline fun g() { h() }

inline fun h() { dsadsaadsf() }

fun box(): String {
    f()

    return "OK"
}