package foo

fun box(): String {
    assertEquals(10, jsCode<Int>("10"))
    assertEquals("10", jsCode<String>("'10'"))
    assertNotEquals("10", jsCode<Int>("10"))
    assertEquals(true, jsCode<Boolean>("true"))
    assertEquals(false, jsCode<Boolean>("false"))
    assertArrayEquals(array(1, 2, 3), jsCode<Array<Int>>("[1, 2, 3]"))

    return "OK"
}