package foo

// CHECK_CONTAINS_NO_CALLS: setValueImplicitThis
// CHECK_CONTAINS_NO_CALLS: setValueExplicitThis

class A(var value: Int)

fun setValueImplicitThis(a: A, newValue: Int) {
    with (a) {
        value = newValue
    }
}

fun setValueExplicitThis(a: A, newValue: Int) {
    with (a) {
        this.value = newValue
    }
}

fun box(): String {
    val a = A(0)
    assertEquals(0, a.value)

    setValueImplicitThis(a, 10)
    assertEquals(10, a.value)

    setValueExplicitThis(a, 20)
    assertEquals(20, a.value)

    return "OK"
}