package org.jetbrains.kotlin.gradle.tasks

internal sealed class ChangesEither {
    internal class Known(val fqNames: Iterable<String>) : ChangesEither()
    internal class Unknown : ChangesEither()
}