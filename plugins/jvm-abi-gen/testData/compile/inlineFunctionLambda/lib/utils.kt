package lib

inline fun sum(x: Int, y: Int): Int {
    val fn: (Int, Int) -> Int = { a, b -> a + b }
    return fn(x, y)
}