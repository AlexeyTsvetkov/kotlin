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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import java.io.File

class KotlinMultiPlatformPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val javaBasePlugin = project.plugins.apply(JavaBasePlugin::class.java)
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)

        val sourceSets = javaPluginConvention.sourceSets
        sourceSets.clear()

        val commonSourceSet = sourceSets.create("common")
        val jvmSourceSet = sourceSets.create("jvm")
        val jsSourceSet = sourceSets.create("js")

        commonSourceSet.allSource.srcDirs += File("src/common")
        jvmSourceSet.allSource.srcDirs += File("src/jvm")
        jsSourceSet.allSource.srcDirs += File("src/js")
        jvmSourceSet.allSource.source(commonSourceSet.allSource)
        jsSourceSet.allSource.source(commonSourceSet.allSource)

        K2MetadataCompiler

        project.afterEvaluate {

        }
    }
}