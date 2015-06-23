import utils.*

fun box(): String {
    assertEquals("log: 10", format())
    assertEquals("log: 20", format(x = 20))
    assertEquals("test: 10", format(message = "test"))
    assertEquals("test: 20", format(message = "test", x = 20))

    return "OK"
}