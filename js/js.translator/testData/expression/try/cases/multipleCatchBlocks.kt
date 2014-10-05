package foo

class UserException() : RuntimeException()

fun bar(e: Exception): String {
    var s: String = ""
    var exceptionObject: Exception? = null;
    try {
        throw e
    }
    catch (e: UserException) {
        s += "UserException"
        exceptionObject = e
    }
    catch (e: IllegalArgumentException) {
        s += "IllegalArgumentException"
        exceptionObject = e
    }
    catch (e: IllegalArgumentException) {
        s += "IllegalArgumentException"
        exceptionObject = e
    }
    catch (e: IllegalArgumentException) {
        s += "IllegalArgumentException"
        exceptionObject = e
    }
    catch (e: IllegalStateException) {
        s += "IllegalStateException"
        exceptionObject = e
    }
    catch (e1: Exception) {
        s += "Exception"
        exceptionObject = e1
    }

    assertEquals(e, exceptionObject, "e == exceptionObject")
    return s
}

fun box(): String {

    assertEquals("UserException", bar(UserException()))
    assertEquals("IllegalArgumentException", bar(IllegalArgumentException()))
    assertEquals("IllegalStateException", bar(IllegalStateException()))
    assertEquals("Exception", bar(Exception()))

    return "OK"
}
