open class A {
    protected open fun foo() {}

    public open fun bar(a: A, b: B, c: C, d: D, e: E) {
        a.foo()
        b.foo()
        c.foo()
        d.foo()
        e.foo()
    }
}

open class B : A() {
    override fun foo() {}

    override fun bar(a: A, b: B, c: C, d: D, e: E) {
        a.foo()
        b.foo()
        c.foo()
        d.foo()
        e.foo()
    }
}

class C : B() {
    override fun bar(a: A, b: B, c: C, d: D, e: E) {
        a.foo()
        b.foo()
        c.foo()
        d.foo()
        e.foo()
    }
}

class D : A() {
    override fun bar(a: A, b: B, c: C, d: D, e: E) {
        a.foo()
        b.foo()
        c.foo()
        d.foo()
        e.foo()
    }
}

class E : A() {
    override fun bar(a: A, b: B, c: C, d: D, e: E) {
        a.foo()
        b.foo()
        c.foo()
        d.foo()
        e.foo()
    }
}

fun box(): String {
    val a = A()
    val b = B()
    val c = C()
    val d = D()
    val e = E()

    a.bar(a, b, c, d, e)
    b.bar(a, b, c, d, e)
    c.bar(a, b, c, d, e)
    d.bar(a, b, c, d, e)
    e.bar(a, b, c, d, e)

    return "OK"
}