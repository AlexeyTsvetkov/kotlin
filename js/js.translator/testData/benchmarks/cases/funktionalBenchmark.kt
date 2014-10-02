package foo

class Countable {
    var count = 0
}

inline fun inc(countable: Countable) = countable.count++

inline fun apply<T, R>(func: (T) -> R, arg: T): R = func(arg)

inline fun run(func: () -> Unit) = func()

inline fun runN(n: Int, func: () -> Unit) { for (i in 1..n) func() }

fun box(): String {
    val c = Countable()

    runN(100000, { inc(c) })

    assertEquals(100000, c.count)

    return "OK"
}