/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.build

import java.io.Serializable
import java.util.*

/**
 * IMPORTANT!
 * This file can be written by one plugin version and read by another.
 * Serializable can automatically handle some changes (such as adding a nullable field).
 * Full info on compatible and incompatible changes: http://docs.oracle.com/javase/8/docs/platform/serialization/spec/version.html
 *
 * TL;DR:
 *  1. Do not change type of a field.
 *  2. Do not change declaration order of fields.
 *  3. Do not delete a field.
 *  4. Nullable field can be added.
 */
data class BaseBuildMetaInfo(
        var isEAP: Boolean? = null,
        var compilerBuildVersion: String? = null,
        var languageVersionString: String? = null,
        var apiVersionString: String? = null,
        var coroutinesStatus: CoroutinesStatus? = null,
        var multiplatformStatus: MultiplatformStatus? = null,
        var ownVersion: Int = BaseBuildMetaInfo.OWN_VERSION
) : Serializable {
    companion object {
        /** DO NOT CHANGE unless you want InvalidClassException to be thrown! */
        const val serialVersionUID: Long = 0
        const val OWN_VERSION: Int = 0
    }
}

data class JvmBuildMetaInfo(
        var baseInfo: BaseBuildMetaInfo? = null,
        var metadataVersionNumbers: IntArray? = null,
        var binaryVersionNumbers: IntArray? = null,
        var ownVersion: Int = JvmBuildMetaInfo.OWN_VERSION
) : Serializable {
    override fun equals(other: Any?): Boolean =
            other is JvmBuildMetaInfo
            && baseInfo == other.baseInfo
            && Arrays.equals(metadataVersionNumbers, other.metadataVersionNumbers)
            && Arrays.equals(binaryVersionNumbers, other.binaryVersionNumbers)
            && ownVersion == other.ownVersion

    override fun hashCode(): Int {
        var code = baseInfo!!.hashCode()
        code = 31 * code + metadataVersionNumbers!!.contentHashCode()
        code = 31 * code + binaryVersionNumbers!!.contentHashCode()
        code = 31 * code + ownVersion
        return code
    }

    companion object {
        /** DO NOT CHANGE unless you want InvalidClassException to be thrown! */
        const val serialVersionUID: Long = 0
        const val OWN_VERSION: Int = 0
    }
}

data class CoroutinesStatus(
        var enable: Boolean? = null,
        var warn: Boolean? = null,
        var error: Boolean? = null,
        var version: Int = CoroutinesStatus.OWN_VERSION
) : Serializable {
    companion object {
        /** DO NOT CHANGE unless you want InvalidClassException to be thrown! */
        const val serialVersionUID: Long = 0
        const val OWN_VERSION: Int = 0
    }
}

data class MultiplatformStatus(
        var enable: Boolean? = null,
        var version: Int = MultiplatformStatus.OWN_VERSION
) : Serializable {
    companion object {
        /** DO NOT CHANGE unless you want InvalidClassException to be thrown! */
        const val serialVersionUID: Long = 0
        const val OWN_VERSION: Int = 0
    }
}