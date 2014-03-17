package foo

fun jsCode(code : String) {}

fun assertEquals<T>(expected: T, actual: T) {
    if (expected != actual) throw Exception("expected: $expected, actual: $actual")
}


fun jsNativeCodeTest(): Int {
    jsCode("""
        var a = 0;
        var b = 0.0;
    """)
}

fun box(): String {
    assertEquals(0, jsNativeCodeTest())

    return "OK"
}