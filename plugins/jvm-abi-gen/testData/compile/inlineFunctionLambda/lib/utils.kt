package lib

inline fun multiplier(x: Int): (Int) -> Int = { y -> x * y }

inline fun multiplier2(x: Int): (Int) -> ((Int) -> Int) = { y -> { z -> x * y * z } }

