package kotlin

fun failsClassCast(message: String, fn: ()->Unit) {
    try {
        fn()
    }
    catch (e: ClassCastException) {
        return
    }

    throw Exception("Expected ClassCastException to be thrown: message=$message")
}