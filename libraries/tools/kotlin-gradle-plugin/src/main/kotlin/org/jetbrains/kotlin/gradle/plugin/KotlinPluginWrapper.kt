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

import org.apache.tools.ant.taskdefs.Classloader
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlugin
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_COMPILER_EMBEDDABLE
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility
import org.jetbrains.kotlin.gradle.utils.loadPropertyFromResources
import java.io.File
import java.net.URLClassLoader
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class KotlinBasePluginWrapper(
    protected val fileResolver: FileResolver
) : Plugin<Project> {
    private val log = Logging.getLogger(this.javaClass)
    val kotlinPluginVersion = loadKotlinVersionFromResource(log)

    open val projectExtensionClass: KClass<out KotlinProjectExtension> get() = KotlinProjectExtension::class

    internal open fun kotlinSourceSetFactory(project: Project): NamedDomainObjectFactory<KotlinSourceSet> =
        DefaultKotlinSourceSetFactory(project, fileResolver)

    override fun apply(project: Project) {
        checkGradleCompatibility()

        project.configurations.maybeCreate(COMPILER_CLASSPATH_CONFIGURATION_NAME).defaultDependencies {
            it.add(project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:$kotlinPluginVersion"))
        }
        project.configurations.maybeCreate(PLUGIN_CLASSPATH_CONFIGURATION_NAME)
        project.configurations.maybeCreate(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
            isTransitive = false
        }

        // TODO: consider only set if if daemon or parallel compilation are enabled, though this way it should be safe too
        System.setProperty(org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")
        val kotlinGradleBuildServices = KotlinGradleBuildServices.getInstance(project.gradle)

        kotlinGradleBuildServices.detectKotlinPluginLoadedInMultipleProjects(project, kotlinPluginVersion)

        project.createKotlinExtension(projectExtensionClass).apply {
            fun kotlinSourceSetContainer(factory: NamedDomainObjectFactory<KotlinSourceSet>) =
                project.container(KotlinSourceSet::class.java, factory)

            project.kotlinExtension.sourceSets = kotlinSourceSetContainer(kotlinSourceSetFactory(project))
        }

        val plugin = getPlugin(project, kotlinGradleBuildServices)

        setupAttributeMatchingStrategy(project)

        plugin.apply(project)
    }

    private fun setupAttributeMatchingStrategy(project: Project) = with(project.dependencies.attributesSchema) {
        KotlinPlatformType.setupAttributesMatchingStrategy(this)
        KotlinUsages.setupAttributesMatchingStrategy(this)
        ProjectLocalConfigurations.setupAttributesMatchingStrategy(this)
    }

    internal abstract fun getPlugin(
        project: Project,
        kotlinGradleBuildServices: KotlinGradleBuildServices
    ): Plugin<Project>

    internal companion object {
        internal val context: Context by lazy {
            val pluginVersion = loadPropertyFromResources("project.properties", "project.version")
            Context(pluginVersion)
        }

        /**
         * DO NOT save references to build specific objects (this may cause leaks!).
         * The lifetime is bound to the lifetime of plugin classes.
         */
        internal class Context(val pluginVersion: String) {
            @Volatile
            private var compilerClassloader: ClassLoader? = null

            @Volatile
            private var jdkClassesRoots: List<File>? = null

            @Volatile
            private var isJsLib: ((File) -> Boolean)? = null

            @Volatile
            private var discoverScriptExtensions: ((File) -> List<String>)? = null

            /**
             * Gets classloader for the compiler with the same version as the plugin's
             */
            @Synchronized
            fun getCompilerClassloader(project: Project): ClassLoader {
                if (compilerClassloader != null) return compilerClassloader!!

                val compilerArtifact = "$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:$pluginVersion"
                val log = project.logger
                log.kotlinDebug { "Trying to resolve '$compilerArtifact' artifact" }

                val projects = generateSequence(project) { project.parent }.toList()

                for (p in projects) {
                    val buildscript = p.buildscript
                    val dependency = buildscript.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:$pluginVersion")
                    val configuration = buildscript.configurations.detachedConfiguration(dependency)

                    try {
                        val files = configuration.resolve()
                        val urls = files.map { it.toURI().toURL() }.toTypedArray()
                        val systemClassloader = File::class.java.classLoader
                        return URLClassLoader(urls, systemClassloader).also { compilerClassloader = it }
                    } catch (e: Throwable) {
                        log.kotlinDebug { "Could not resolve '$compilerArtifact' in '${project.path}' buildscript: $e" }
                    }
                }

                error(
                    "Could not resolve '$compilerArtifact' using buildscript dependencies of the following projects: " +
                            "\n${projects.joinToString("\n")}"
                )
            }

            @Synchronized
            fun getJdkClassesRoots(project: Project): List<File> {
                val calculatedRoots = jdkClassesRoots
                if (calculatedRoots != null) return calculatedRoots

                val compilerCL = getCompilerClassloader(project)
                val pathUtilClass = compilerCL.loadClass("org.jetbrains.kotlin.utils.PathUtil")
                val m = pathUtilClass.getMethod("getJdkClassesRootsFromCurrentJre", pathUtilClass)

                @Suppress("UNCHECKED_CAST")
                return (m.invoke(pathUtilClass) as List<File>).also { jdkClassesRoots = it }
            }

            @Synchronized
            fun isKotlinJsLibraryFun(project: Project): (File) -> Boolean {
                if (isJsLib == null) {
                    val compilerCL = getCompilerClassloader(project)
                    val libraryUtils = compilerCL.loadClass("org.jetbrains.kotlin.utils.LibraryUtils")
                    val m = libraryUtils.getMethod("isKotlinJavascriptLibrary", libraryUtils, File::class.java)
                    isJsLib = { f: File -> m.invoke(libraryUtils, f) as Boolean }
                }

                return isJsLib!!
            }

            @Synchronized
            fun discoverScriptExtensionsFun(project: Project): (File) -> List<String> {
                if (discoverScriptExtensions == null) {
                    val compilerCL = getCompilerClassloader(project)
                    val libraryUtils = compilerCL.loadClass("org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource")
                    val m = libraryUtils.getMethod("discoverScriptExtensionsFromGradle", libraryUtils, File::class.java)
                    discoverScriptExtensions = { f: File ->
                        @Suppress("UNCHECKED_CAST")
                        m.invoke(libraryUtils, f) as List<String>
                    }
                }

                return discoverScriptExtensions!!
            }
        }
    }
}

open class KotlinPluginWrapper @Inject constructor(
    fileResolver: FileResolver,
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinPlugin(kotlinPluginVersion, registry)

    override val projectExtensionClass: KClass<out KotlinJvmProjectExtension>
        get() = KotlinJvmProjectExtension::class
}

open class KotlinCommonPluginWrapper @Inject constructor(
    fileResolver: FileResolver,
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinCommonPlugin(kotlinPluginVersion, registry)

    override val projectExtensionClass: KClass<out KotlinCommonProjectExtension>
        get() = KotlinCommonProjectExtension::class
}

open class KotlinAndroidPluginWrapper @Inject constructor(
    fileResolver: FileResolver,
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinAndroidPlugin(kotlinPluginVersion, registry)

    override val projectExtensionClass: KClass<out KotlinAndroidProjectExtension>
        get() = KotlinAndroidProjectExtension::class
}

open class Kotlin2JsPluginWrapper @Inject constructor(
    fileResolver: FileResolver,
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        Kotlin2JsPlugin(kotlinPluginVersion, registry)

    override val projectExtensionClass: KClass<out Kotlin2JsProjectExtension>
        get() = Kotlin2JsProjectExtension::class
}

open class KotlinJsPluginWrapper @Inject constructor(
    fileResolver: FileResolver
) : KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinJsPlugin(kotlinPluginVersion)

    override val projectExtensionClass: KClass<out KotlinJsProjectExtension>
        get() = KotlinJsProjectExtension::class
}

open class KotlinMultiplatformPluginWrapper @Inject constructor(
    fileResolver: FileResolver,
    private val featurePreviews: FeaturePreviews
) : KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin(project: Project, kotlinGradleBuildServices: KotlinGradleBuildServices): Plugin<Project> =
        KotlinMultiplatformPlugin(
            kotlinPluginVersion,
            featurePreviews
        )

    override val projectExtensionClass: KClass<out KotlinMultiplatformExtension>
        get() = KotlinMultiplatformExtension::class
}

fun Project.getKotlinPluginVersion(): String? =
    plugins.asSequence().mapNotNull { (it as? KotlinBasePluginWrapper)?.kotlinPluginVersion }.firstOrNull()

fun Plugin<*>.loadKotlinVersionFromResource(log: Logger): String {
    log.kotlinDebug("Loading version information")
    val projectVersion = loadPropertyFromResources("project.properties", "project.version")
    log.kotlinDebug("Found project version [$projectVersion]")
    return projectVersion
}
