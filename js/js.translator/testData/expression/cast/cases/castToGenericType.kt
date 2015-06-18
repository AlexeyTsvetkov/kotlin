package foo

class A(val s: String)

fun castsNotNullToNullableT<T>(a: Any) {
    a as T
    a as T?
    a as? T
    a as? T?
}

fun castsNullableToNullableT<T>(a: Any?) {
    a as T
    a as T?
    a as? T
    a as? T?
}


fun castsNotNullToNotNullT<T : Any>(a: Any) {
    a as T
    a as T?
    a as? T
    a as? T?
}

fun castNullableToNotNullT<T : Any>(a: Any?) {
    a as T
}

fun castsNullableToNotNullT<T : Any>(a: Any?) {
    a as T?
    a as? T
    a as? T?
}

fun box(): String {
    val a = A("OK")

    castsNotNullToNullableT<A>(a)
    castsNullableToNullableT<A>(a)
    castsNullableToNullableT<A>(null)
    castsNotNullToNotNullT<A>(a)
    castsNullableToNotNullT<A>(a)
    castsNullableToNotNullT<A>(null)
    castNullableToNotNullT<A>(a)
    failsClassCast("castNullableToNotNullT<A>(null)") { castNullableToNotNullT<A>(null) }

    return "OK"
}
