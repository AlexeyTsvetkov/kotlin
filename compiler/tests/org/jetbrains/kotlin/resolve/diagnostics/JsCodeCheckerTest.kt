/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.diagnostics

import java.io.File
import org.jetbrains.kotlin.utils.fileUtils.*
import org.jetbrains.kotlin.resolve.diagnostics.*

import org.junit.Test
import org.junit.Assert

class JsCodeCheckerTest {
    private val TEST_DIR = "compiler/testData/diagnostics/tests/jsCode/"
    private val TEST_EXTENSION = ".test"
    private val EXPECT_EXTENSION = ".expect"
    private val UNDERLINE = "@"

    Test
    fun underlineTest() {
        val dir = File(TEST_DIR + "underline/")
        val files = dir.listFiles()
        if (files == null) throw AssertionError("no files")

        val test = hashMapOf<String, File>()
        val expect = hashMapOf<String, File>()

        for (file in files) {
            val name = file.getName()
            val matchedExtension: String
            val mapToAdd: MutableMap<String, File>

            if (name.endsWith(TEST_EXTENSION)) {
                mapToAdd = test
                matchedExtension = TEST_EXTENSION
            }
            else if (name.endsWith(EXPECT_EXTENSION)) {
                mapToAdd = expect
                matchedExtension = EXPECT_EXTENSION
            }
            else {
                throw IllegalStateException("Unknown file extension: " + file)
            }

            val baseName = name.substring(0, name.length() - matchedExtension.length())
            mapToAdd.put(baseName, file)
        }


        for (testName in test.keySet()) {
            doUnderlineTest(test.get(testName), expect.get(testName))
        }
    }

    private fun doUnderlineTest(testFile: File, expectFile: File) {
        val test = testFile.readTextOrEmpty()
        val expected = expectFile.readTextOrEmpty()
        val testWithoutTags = test.replaceAll(UNDERLINE, "")

        val underlineTags = test.indices.filter { test.charAt(it).toString() == UNDERLINE }
        assert(underlineTags.size() >= 2) { "not enough underline tags" }
        val start = underlineTags.get(0) - UNDERLINE.length() // do not count tag itself
        val end = underlineTags.get(1) - UNDERLINE.length()*2

        val actual = TestStringUtils.underlineString(testWithoutTags, start, end)
        Assert.assertEquals(expected.trim(), actual.trim())
    }
}
