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

import com.intellij.util.PathUtil
import org.jetbrains.jps.builders.BuildResult
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.kotlin.test.JetTestUtils

public class SimpleKotlinJpsBuildTest : AbstractKotlinJpsBuildTestCase() {

    override fun setUp() {
        super.setUp()
        workDir = JetTestUtils.tmpDirForTest(this)
    }

    public fun testThreeModulesNoReexport() {
        val aFile = createFile("a/a.kt",
                               """
                                   trait A1 {
                                       fun bar()
                                   }
                                   trait A2 {
                                       fun bar()
                                   }
                               """)
        val a = addModule("a", PathUtil.getParentPath(aFile))

        val bFile = createFile("b/b.kt",
                               """
                                    trait B1 {
                                        fun foo(): B2? = null
                                    }

                                    trait B2 : A1, A2 {
                                        override fun bar() {}
                                    }
                               """)
        val b = addModule("b", PathUtil.getParentPath(bFile))
        JpsJavaExtensionService.getInstance().getOrCreateDependencyExtension(
                b.getDependenciesList().addModuleDependency(a)
        ).setExported(false)

        val cFile = createFile("c/c.kt",
                               """
                                    class C : B1 {
                                        fun test() {
                                            foo()?.bar()
                                        }
                                    }
                               """)
        val c = addModule("c", PathUtil.getParentPath(cFile))
        c.getDependenciesList().addModuleDependency(b)

        addKotlinRuntimeDependency()
        rebuildAll()
    }

    public fun testLoadingKotlinFromDifferentModules() {
        val aFile = createFile("m1/K.kt",
                               """
                                   package m1;

                                   trait K {
                                   }
                               """)
        createFile("m1/J.java",
                               """
                                   package m1;

                                   public interface J {
                                       K bar();
                                   }
                               """)
        val a = addModule("m1", PathUtil.getParentPath(aFile))

        val bFile = createFile("m2/m2.kt",
                               """
                                    import m1.J;
                                    import m1.K;

                                    trait M2: J {
                                        override fun bar(): K
                                    }
                               """)
        val b = addModule("b", PathUtil.getParentPath(bFile))
        JpsJavaExtensionService.getInstance().getOrCreateDependencyExtension(
                b.getDependenciesList().addModuleDependency(a)
        ).setExported(false)

        addKotlinRuntimeDependency()
        rebuildAll()
    }

    public fun testProgressMessagesJvm() {
        val progressMessages = makeSimpleModuleAndGetProgressMessages {
            addKotlinRuntimeDependency()
        }

        assertContainsOrdered(progressMessages,
                              "Started building Module 'm' production",
                              "Loading plugins [m]",
                              "Configuring the compilation environment [m]",
                              "Analyzing file: Foo.kt [m]",
                              "Generating bytecode: Foo.kt [m]",
                              "Writing bytecode [m]",
                              "Finished building Module 'm' production")
    }

    public fun testProgressMessagesJs() {
        val progressMessages = makeSimpleModuleAndGetProgressMessages {
            addKotlinJavaScriptStdlibDependency()
        }

        assertContainsOrdered(progressMessages,
                              "Started building Module 'm' production",
                              "Analyzing file: Foo.kt [m]",
                              "Generating JavaScript: Foo.kt [m]",
                              "Inlining functions [m]",
                              "Writing JavaScript [m]",
                              "Finished building Module 'm' production")
    }

    private fun makeSimpleModuleAndGetProgressMessages(addDepedencies: ()->Unit): Collection<String> {
        val fooKt = createFile("m/Foo.kt",
                               """
                                   package m;

                                   interface Foo
                               """)
        addModule("m", PathUtil.getParentPath(fooKt))
        addDepedencies()
        return makeAll().getMessages(BuildMessage.Kind.PROGRESS)
                .filter { it.getKind() == BuildMessage.Kind.PROGRESS }
                .map { it.getMessageText() }
    }
}
