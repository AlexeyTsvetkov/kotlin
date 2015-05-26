import lib.*

// MODULE: m0
// FILE: a.kt

fun three(): Int = <!COULD_NOT_INLINE_FROM_LIBRARY!>sum(1, 2)<!>