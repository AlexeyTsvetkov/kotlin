package foo

// CHECK_NOT_CALLED: filterIsInstance

data class A(val x: Int)

data class B(val x: Int)

inline fun<reified T> filterIsInstance(arrayOfAnys: Array<Any>): List<T> {
    return arrayOfAnys.filter { it is T }.map { it as T }
}

fun box(): String {
    val src: Array<Any> = arrayOf(A(1), B(2), A(3), B(4))

    assertEquals(listOf(A(1), A(3)), filterIsInstance<A>(src))
    assertEquals(listOf(B(2), B(4)), filterIsInstance<B>(src))

    return "OK"
}
