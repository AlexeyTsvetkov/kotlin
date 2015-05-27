package foo

// CHECK_NOT_CALLED: set

class X {
    var log = ""

    inline fun set<T>(key: T, value: T) {
        log += "set($key, $value);"
    }
}

fun box(): String {
    val x = X()
    x[1] = 2
    x[3] = 4
    assertEquals("set(1, 2);set(3, 4);", x.log)

    return "OK"
}