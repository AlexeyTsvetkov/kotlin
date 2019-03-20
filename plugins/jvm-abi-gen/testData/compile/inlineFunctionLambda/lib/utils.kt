package lib

import java.util.function.Function

inline fun multiplier(x: Int): (Int) -> Int = { y -> x * y }

inline fun multiplier2(x: Int): (Int) -> ((Int) -> Int) = { y -> { z -> x * y * z } }

inline fun multiplierObject(x: Int): Function<Int, Int> =
    object : Function<Int, Int> {
        override fun apply(y: Int) = x * y
    }
