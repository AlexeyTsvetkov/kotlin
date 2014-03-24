package foo

fun abs(n: Int): Int = jsCode("""
    if (n >= 0) {
        return n;
    } else {
        return -n;
    }
""")

fun take(count: Int, a: Array<Int>): Array<Int> = jsCode("""
    res = [];

    var i = 0;
    while (i < a.length) {
        if (i >= count) {
            break;
        }

        res.push(a[i]);
        i++;
    }

    return res;
""")

fun filter(pred: (Int) -> Boolean, a: Array<Int>): Array<Int> = jsCode("""
    res = [];

    for (var i = 0; i < a.length; i++) {
        if (!pred(a[i])) {
            continue;
        }

        res.push(a[i]);
    }

    return res;
""")

fun box(): String {
    assertEquals(1, abs(1))
    assertEquals(1, abs(-1))
    assertArrayEquals(array(1, 2), take(2, array(1,2,3)))

    var isEven: (Int) -> Boolean = {(x) -> x % 2 == 0; };
    assertArrayEquals(array(2, 4), filter(isEven, array(1,2,3,4)))

    return "OK"
}