package foo

/* Note: impossible to use jsCode to create object that
 * is to implement/override a virtual function because of name mangling.
 *
 * For example: if Summizer.sum was abstract,
 * the following code would not compile
 */

class Summizer {
    fun sum(a: Int, b: Int): Int { return a + b }
}

fun getSummizer(): Summizer = jsCode("""
    var summizer = {
        sum: function(a, b) { return a + b;}
    };

    return summizer;
""");

fun box(): String {
    val summizer = getSummizer()
    assertEquals(3, summizer.sum(1, 2))

    return "OK"
}