package foo

// Function is written this way to prevent inlining as expression
inline fun sum(x: Int, y: Int): Int {
    var sum = x
    val sign = if (y < 0) -1 else 1
    var yabs = y * sign

    while (yabs > 0) {
        sum += sign
        yabs--
    }

    return sum
}