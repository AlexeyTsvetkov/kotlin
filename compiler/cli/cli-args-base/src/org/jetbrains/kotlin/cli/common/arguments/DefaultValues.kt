/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.LanguageVersion

open class DefaultValues(val defaultValue: String, val possibleValues: List<String>? = null) {
    object BooleanFalseDefault : DefaultValues("false")

    object BooleanTrueDefault : DefaultValues("true")

    object StringNullDefault : DefaultValues("null")

    object ListEmptyDefault : DefaultValues("<empty list>")

    object LanguageVersions : DefaultValues(
            "null",
            LanguageVersion.values().map { "\"${it.description}\"" }
    )
}
