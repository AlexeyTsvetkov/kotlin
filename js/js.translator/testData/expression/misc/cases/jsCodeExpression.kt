package foo

class A(a: Int) {
    val value = a;
}

fun box(): String {
    assertEquals(3, jsCode<Int>("2 + 1"))
    assertEquals(1, jsCode<Int>("2 - 1"))
    assertEquals(10, jsCode<Int>("2 * 5"))
    assertEquals(5, jsCode<Int>("10 / 2"))

    assertEquals(4, jsCode<Int>("1 << 2"))
    assertEquals(1, jsCode<Int>("4 >> 2"))
    assertEquals(0, jsCode<Int>("0 & 1"))
    assertEquals(1, jsCode<Int>("0 | 1"))
    assertEquals(1, jsCode<Int>("0 ^ 1"))
    assertEquals(-2, jsCode<Int>("~1"))

    assertEquals("12", jsCode<String>("'1' + '2'"))

    assertEquals(true , jsCode<Boolean>("true || false"))
    assertEquals(false , jsCode<Boolean>("true && false"))
    assertEquals(false , jsCode<Boolean>("!true"))

    val sum: (Int, Int) -> Int = { (a, b) -> a + b }
    assertEquals(6, jsCode<Int>("sum(sum(1, 2), 3)"))

    val ar = array(1, 2, 3)
    assertEquals(3, jsCode<Int>("ar[2]"))

    val a = A(5)
    assertEquals(5, jsCode<Int>("a.value"))

    return "OK"
}