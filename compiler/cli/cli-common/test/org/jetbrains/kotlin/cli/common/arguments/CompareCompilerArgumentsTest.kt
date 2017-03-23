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

package org.jetbrains.kotlin.cli.common.arguments

import org.junit.Test

import org.junit.Assert.*

class CompareCompilerArgumentsTest {
    @Test
    fun testJvmArguments() {
        checkArguments(jvmArgs {}, jvmArgs {}, null)
        checkArguments(jvmArgs { jvmTarget = "1.6" }, jvmArgs { jvmTarget = "1.8" }, "jvmTarget")
        checkArguments(jvmArgs { classpath = "" }, jvmArgs { jvmTarget = "some.jar" }, "classpath")
        checkArguments(jvmArgs { languageVersion = "1.0" }, jvmArgs { languageVersion = "1.1" }, "languageVersion")
        checkArguments(jvmArgs { apiVersion = "1.0" }, jvmArgs { apiVersion = "1.1" }, "apiVersion")
    }

    private fun jvmArgs(fn: K2JVMCompilerArguments.()->Unit): K2JVMCompilerArguments =
            K2JVMCompilerArguments().apply(fn)

    private fun checkArguments(arg1: K2JVMCompilerArguments, arg2: K2JVMCompilerArguments, expectedDifferentPropertyName: String?) {
        assertEquals(expectedDifferentPropertyName, arg1.firstDifferentArgument(arg2)?.name)
    }
}