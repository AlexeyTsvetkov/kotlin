package foo

fun forSumRange(a: Int, b: Int): Int = jsCode("""
    var sum = 0;

    for(var i = a; i <= b; i++) {
        sum += i;
    }

    return sum;
""")

fun whileSumRange(a: Int, b: Int): Int = jsCode("""
    var sum = 0;

    var i = a;
    while (i <= b) {
        sum += i;
        i++
    }

    return sum;
""")

fun doWhileSumRange(a: Int, b: Int): Int = jsCode("""
    var sum = 0;

    var i = a;
    do {
        sum += i;
        i++
    } while (i <= b);

    return sum;
""")

fun kotlinSumRange(a: Int, b: Int): Int {
    var sum = 0

    for (i in a..b) {
        sum += i
    }

    return sum
}

fun box(): String {
    assertEquals(kotlinSumRange(1, 10), forSumRange(1, 10), "forSumRange")
    assertEquals(kotlinSumRange(1, 10), whileSumRange(1, 10), "whileSumRange")
    assertEquals(kotlinSumRange(1, 10), doWhileSumRange(1, 10), "doWhileSumRange")

    return "OK"
}