package foo

// CHECK_NOT_CALLED: doLet

inline fun <T : Any, R> T.doLet(f: (T) -> R): R = f(this)

fun streamFromFunctionWithInitialValue() {
    val values = makeStream(3) { n -> if (n > 0) n - 1 else null }
    assertEquals(arrayListOf(3, 2, 1, 0), values.toList())
}

fun <T : Any> makeStream(initialValue: T, nextFunction: (T) -> T?): Stream<T> =
        stream(nextFunction.generateFrom(initialValue))

fun <T: Any> Function1<T, T?>.generateFrom(initialValue: T): Function0<T?> {
    var nextValue: T? = initialValue

    return {
        nextValue?.doLet { result ->
            nextValue = this@generateFrom(result)
            result
        }
    }
}

fun box(): String {
    val values = makeStream(3) { n -> if (n > 0) n - 1 else null }
    assertEquals(arrayListOf(3, 2, 1, 0), values.toList())

    return "OK"
}