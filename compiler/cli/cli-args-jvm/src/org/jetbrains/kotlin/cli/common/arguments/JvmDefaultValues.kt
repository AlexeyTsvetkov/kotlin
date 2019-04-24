/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.JvmTarget

object JvmDefaultValues {
    object JvmTargetVersions : DefaultValues(
        "\"" + JvmTarget.DEFAULT.description + "\"",
        JvmTarget.values().map { "\"${it.description}\"" }
    )
}