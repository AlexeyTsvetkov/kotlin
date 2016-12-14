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

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinTask

abstract class KotlinPlatformPluginBase(protected val platformName: String) : Plugin<Project>

open class KotlinPlatformCommonPlugin : KotlinPlatformPluginBase("common") {
    override fun apply(project: Project) {
        project.applyPlugin<KotlinCommonPluginWrapper>()
    }
}

open class KotlinPlatformImplementationPluginBase(platformName: String) : KotlinPlatformPluginBase(platformName) {
    private val commonProjects = arrayListOf<Project>()
    private val platformSourceSets = hashMapOf<String, SourceSet>()

    override fun apply(project: Project) {
        project.tasks.withType(AbstractKotlinTask::class.java).all {
            (it as KotlinCompile<*>).kotlinOptions.freeCompilerArgs += listOf("-Xmulti-platform")
        }

        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.forEach {
            platformSourceSets[it.name] = it
        }

        val kotlinCommonConfig = project.configurations.create("kotlinCommon")
        kotlinCommonConfig.isTransitive = false
        kotlinCommonConfig.dependencies.whenObjectAdded { dep ->
            if (dep is ProjectDependency) {
                addCommonProject(dep.dependencyProject, project)
            }
        }
    }

    private fun addCommonProject(commonProject: Project, platformProject: Project) {
        commonProjects.add(commonProject)
        if (commonProjects.size > 1) {
            throw GradleException("Platform project $platformProject implements more than one common project: ${commonProjects.joinToString()}")
        }

        commonProject.whenEvaluated {
            if ((!commonProject.plugins.hasPlugin(KotlinPlatformCommonPlugin::class.java))) {
                throw GradleException("Platform project $platformProject implements non-common project $commonProject (`apply plugin 'kotlin-platform-kotlin'`)")
            }

            val javaPluginConvention = commonProject.convention.getPlugin(JavaPluginConvention::class.java)
            javaPluginConvention.sourceSets.all { commonSourceSet ->
                // todo: warn if not found
                val platformSourceSet = platformSourceSets[commonSourceSet.name]
                val platformKotlinSourceDirectorySet = platformSourceSet?.kotlin
                commonSourceSet.kotlin?.srcDirs?.forEach { platformKotlinSourceDirectorySet?.srcDir(it) }
            }
        }
    }

    private val SourceSet.kotlin: SourceDirectorySet?
            get() = ((getConvention("kotlin") ?: getConvention("kotlin2js")) as? KotlinSourceSet)?.kotlin
}

private inline fun <T> Project.whenEvaluated(crossinline fn: Project.()->T) {
    if (state.executed) {
        fn()
    }
    else {
        afterEvaluate { it.fn() }
    }
}

open class KotlinPlatformJvmPlugin : KotlinPlatformImplementationPluginBase("jvm") {
    override fun apply(project: Project) {
        project.applyPlugin<KotlinPluginWrapper>()
        super.apply(project)
    }
}

open class KotlinPlatformJsPlugin : KotlinPlatformImplementationPluginBase("js") {
    override fun apply(project: Project) {
        project.applyPlugin<Kotlin2JsPluginWrapper>()
        super.apply(project)
    }
}

private inline fun <reified T : Plugin<*>> Project.applyPlugin() {
    pluginManager.apply(T::class.java)
}