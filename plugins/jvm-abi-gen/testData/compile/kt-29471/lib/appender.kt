package lib

import java.util.function.Function

inline fun appender(append: String): Function<String, String> {
    return object : Function<String, String> {
        override fun apply(arg: String) = arg + append
    }
}

inline fun foo(x: Int) = { x }