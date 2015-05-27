package foo

// CHECK_NOT_CALLED: get

class X {
    var log = ""

    inline fun get<T>(key: T): T {
        log += "get($key);"
        return key
    }
}

fun box(): String {
    val x = X()
    assertEquals(1, x[1])
    assertEquals(2, x[2])
    assertEquals("get(1);get(2);", x.log)

    return "OK"
}