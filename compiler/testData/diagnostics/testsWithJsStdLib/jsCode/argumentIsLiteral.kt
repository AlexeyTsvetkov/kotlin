val s = "1 + 1;"
fun two(): Int = <!JSCODE_ARGUMENT_SHOULD_BE_LITERAL!>js<!>(s)
fun three(): Int = <!JSCODE_ARGUMENT_SHOULD_BE_LITERAL!>js<!>("1" + "+ 2;")
