/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.multiproject

import java.io.File

interface ModulesApiHistory {
    fun historyFileForArtifact(file: File): File?
}

object EmptyModulesApiHistory : ModulesApiHistory {
    override fun historyFileForArtifact(file: File): File? = null
}