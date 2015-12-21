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

import org.jetbrains.jps.model.JpsElementFactory
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.JpsSimpleElement
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase
import org.jetbrains.kotlin.incremental.components.LookupTracker
import java.io.File

interface TestingContext {
    companion object {
        internal val TESTING_CONTEXT: JpsElementChildRoleBase<JpsSimpleElement<out TestingContext>> =
                JpsElementChildRoleBase.create("Testing context")

        fun set(project: JpsProject, testingContext: TestingContext) {
            val dataContainer = JpsElementFactory.getInstance().createSimpleElement(testingContext)
            project.container.setChild(TESTING_CONTEXT, dataContainer)
        }

        inline internal fun ifExists(project: JpsProject, fn: (TestingContext)->Unit) {
            if ("true".equals(System.getProperty("kotlin.jps.tests"), ignoreCase = true)) {
                project.container.getChild(TestingContext.TESTING_CONTEXT)?.data?.let(fn)
            }
        }
    }

    val lookupTracker: LookupTracker

    fun registerDirtyFiles(files: Iterable<File>)
}