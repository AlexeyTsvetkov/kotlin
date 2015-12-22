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

package org.jetbrains.kotlin.jps.build

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.jps.incremental.CacheVersionProvider
import java.io.File

abstract class AbstractExperimentalIncrementalJpsTest : AbstractIncrementalJpsTest() {
    override val enableExperimentalIncrementalCompilation = true
}

abstract class AbstractClassHierarchyAffectedTest : AbstractExperimentalIncrementalJpsTest() {
    override fun checkLogs(makeResults: List<MakeResult>) {
        val dirtyFilesLogs = makeResults.map { makeResult ->
            val dirtyFilesNames = makeResult.dirtyFiles.map { it.parentFile.name + "/" + it.name }

            buildString {
                append(makeResult.log)

                if (dirtyFilesNames.isNotEmpty()) {
                    append("Marked dirty by Kotlin builder:\n")
                    append(dirtyFilesNames.sorted().joinToString(separator = "\n"))
                }
            }
        }

        val actualLog = dirtyFilesLogs.withIndex()
                .map { "======== Step #${it.index + 1} ========\n${it.value}" }
                .joinToString(separator = "\n\n")
        val expectedFile = File(testDataDir, AbstractIncrementalJpsTest.BUILD_LOG_FILE_NAME)
        UsefulTestCase.assertSameLinesWithFile(expectedFile.canonicalPath, actualLog)
    }
}

abstract class AbstractExperimentalIncrementalLazyCachesTest : AbstractIncrementalLazyCachesTest() {
    override val enableExperimentalIncrementalCompilation = true

    override val expectedCachesFileName: String
        get() = "experimental-expected-kotlin-caches.txt"
}

abstract class AbstractExperimentalChangeIncrementalOptionTest : AbstractIncrementalLazyCachesTest()

abstract class AbstractExperimentalIncrementalCacheVersionChangedTest : AbstractIncrementalCacheVersionChangedTest() {
    override val enableExperimentalIncrementalCompilation = true

    override fun getVersions(cacheVersionProvider: CacheVersionProvider, targets: Iterable<ModuleBuildTarget>) =
            targets.map { cacheVersionProvider.experimentalVersion(it) }
}

abstract class AbstractDataContainerVersionChangedTest : AbstractExperimentalIncrementalCacheVersionChangedTest() {
    override val experimentalBuildLogFileName = "data-container-version-build.log"

    override fun createExperimentalBuildLog(incrementalMakeResults: List<MakeResult>) =
            createDefaultBuildLog(incrementalMakeResults)

    override fun getVersions(cacheVersionProvider: CacheVersionProvider, targets: Iterable<ModuleBuildTarget>) =
            listOf(cacheVersionProvider.dataContainerVersion())
}