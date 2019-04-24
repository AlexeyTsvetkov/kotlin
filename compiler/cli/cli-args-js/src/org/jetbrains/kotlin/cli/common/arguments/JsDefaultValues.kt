/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

object JsDefaultValues {
    object JsEcmaVersions : DefaultValues(
        "\"v5\"",
        listOf("\"v5\"")
    )

    object JsModuleKinds : DefaultValues(
        "\"plain\"",
        listOf("\"plain\"", "\"amd\"", "\"commonjs\"", "\"umd\"")
    )

    object JsSourceMapContentModes : DefaultValues(
        "null",
        listOf(
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING
        ).map { "\"$it\""}
    )

    object JsMain : DefaultValues(
        "\"" + K2JsArgumentConstants.CALL + "\"",
        listOf("\"" + K2JsArgumentConstants.CALL + "\"", "\"" + K2JsArgumentConstants.NO_CALL + "\"")
    )
}