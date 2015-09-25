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

import org.jetbrains.kotlin.config.IncrementalCompilation

public abstract class AbstractKotlinIncrementalCompilationOffTest : AbstractIncrementalLazyCachesTest() {
    override fun doTest(testDataPath: String) {
        try {
            super.doTest(testDataPath)
        }
        finally {
            IncrementalCompilation.enableIncrementalCompilation()
        }
    }

    override fun performAdditionalModifications(modifications: List<AbstractIncrementalJpsTest.Modification>) {
        super.performAdditionalModifications(modifications)

        var modified = 0

        for (modification in modifications) {
            if (!modification.path.endsWith("incremental-compilation-off")) continue

            when (modification) {
                is AbstractIncrementalJpsTest.ModifyContent -> {
                    IncrementalCompilation.disableIncrementalCompilation()
                }
                is AbstractIncrementalJpsTest.DeleteFile -> {
                    IncrementalCompilation.enableIncrementalCompilation()
                }
                else -> {
                    throw IllegalStateException("Unknown modification type: ${modification.javaClass}")
                }
            }

            modified++
        }

        if (modified > 1) {
            throw IllegalStateException("Incremental compilation was enabled/disable more than once")
        }
    }
}