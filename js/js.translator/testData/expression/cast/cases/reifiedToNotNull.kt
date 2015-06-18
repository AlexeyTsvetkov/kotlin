package foo

// CHECK_NOT_CALLED: test

import java.lang.*

trait A

class AImpl: A

inline
fun test<reified T>(x: Any?): T = x as T

fun box(): String {
    var a: A = AImpl()
    assertEquals(a, test<A>(a), "a = AImpl()")
    a = object : A {}
    assertEquals(a, test<A>(a), "a = object : A{}")
    assertClassCastException("test(null)") { test<A>(null) }
    assertClassCastException("test(object{})") { test<A>(object{}) }

    return "OK"
}