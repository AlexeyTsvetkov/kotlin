/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.util

enum class GradleVersion {
    `2-2-1`,
    `2-3`,
    `2-4`,
    `2-5`,
    `2-6`,
    `2-7`,
    `2-8`,
    `2-9`,
    `2-10`,
    `2-11`;

    override fun toString(): String {
        return super.toString().replace("-", ".")
    }
}