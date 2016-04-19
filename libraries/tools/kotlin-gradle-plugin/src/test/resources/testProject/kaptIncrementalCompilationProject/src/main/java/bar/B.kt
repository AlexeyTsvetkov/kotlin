package bar

@example.ExampleAnnotation
class B {
    @field:example.ExampleAnnotation
    val valB: String = "text"

    @example.ExampleAnnotation
    fun funB() {}
}