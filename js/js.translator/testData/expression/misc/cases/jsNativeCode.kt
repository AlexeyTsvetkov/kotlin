package foo

import js.jsCode

fun assertEquals<T>(expected: T, actual: T) {
    if (expected != actual) throw Exception("expected: $expected, actual: $actual")
}

fun jsNativeCodeTest(mult : Int): Int = jsCode("""
    var a = 0;

    for (var i = 0; i < 5; i++) {
        a = a + i;
    }

    return mult*a;
""")

fun box(): String {
    assertEquals(100, jsNativeCodeTest(10))

    return "OK"
}