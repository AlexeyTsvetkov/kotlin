package foo

import utils.*

fun box(): String {
    if (sum(1, 2) != 3) return "NOT OK"

    return "OK"
}