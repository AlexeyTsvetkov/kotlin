open abstract class A {
    protected abstract val x: Int
    protected fun foo(): Int = x
}

class B : A() {
    override val x: Int = 1
}

class C: A() {
    override val x: Int = 2

    fun box(): String =
            when {
                B().foo() != 1 -> "Fail 1"
                (B() as A).foo() != 1 -> "Fail 2"
                (C()).foo() != 2 -> "Fail 3"
                (C() as A).foo() != 2 -> "Fail 4"
                else -> "OK"
            }
}

fun box(): String = C().box()