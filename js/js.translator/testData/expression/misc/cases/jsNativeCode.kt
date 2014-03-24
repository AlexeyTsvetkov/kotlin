package foo

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