package bar

@example.ExampleAnnotation
class B {
    @field:example.ExampleAnnotation
    val valB = "text"

    @example.ExampleAnnotation
    fun funB() {}
}