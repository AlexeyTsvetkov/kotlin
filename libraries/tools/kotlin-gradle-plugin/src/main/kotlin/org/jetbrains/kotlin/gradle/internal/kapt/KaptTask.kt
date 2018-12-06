package org.jetbrains.kotlin.gradle.internal

import com.intellij.openapi.util.io.FileUtil
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.cacheOnlyIfEnabledForKotlin
import org.jetbrains.kotlin.gradle.tasks.isBuildCacheSupported
import org.jetbrains.kotlin.gradle.utils.isJavaFile
import java.io.File

@CacheableTask
abstract class KaptTask : ConventionTask() {
    init {
        cacheOnlyIfEnabledForKotlin()

        if (isBuildCacheSupported()) {
            val reason = "Caching is disabled by default for kapt because of arbitrary behavior of external " +
                    "annotation processors. You can enable it by adding 'kapt.useBuildCache = true' to the build script."
            outputs.cacheIf(reason) { useBuildCache }
        }
    }

    @get:Internal
    internal lateinit var kotlinCompileTask: KotlinCompile

    @get:Internal
    internal lateinit var stubsDir: File

    @get:Classpath
    @get:InputFiles
    val kaptClasspath: FileCollection
        get() = project.files(*kaptClasspathConfigurations.toTypedArray())

    @get:Classpath
    @get:InputFiles
    val compilerClasspath: List<File>
        get() = kotlinCompileTask.computedCompilerClasspath

    @get:Internal
    internal lateinit var kaptClasspathConfigurations: List<Configuration>

    @get:OutputDirectory
    internal lateinit var classesDir: File

    @get:OutputDirectory
    lateinit var destinationDir: File

    @get:OutputDirectory
    lateinit var kotlinSourcesDestinationDir: File

    @get:Nested
    internal val annotationProcessorOptionProviders: MutableList<Any> = mutableListOf()

    @get:Input
    internal var discoverAnnotationProcessorsInCompileClasspath: Boolean = true

    // @Internal because _abiClasspath and _nonAbiClasspath are used for actual checks
    @get:Internal
    val classpath: FileCollection
        get() = kotlinCompileTask.classpath

    // used only for up-to-date checks
    @Deprecated("_abiClasspath should not be used. Use 'classpath' property instead.")
    @CompileClasspath
    @InputFiles
    internal fun _abiClasspath(): FileCollection =
        kotlinCompileTask.classpath.takeIf { !discoverAnnotationProcessorsInCompileClasspath }.orEmpty()

    // used only for up-to-date checks
    @Deprecated("_nonAbiClasspath should not be used. Use 'classpath' property instead.")
    @Classpath
    @InputFiles
    internal fun _nonAbiClasspath(): FileCollection =
        kotlinCompileTask.classpath.takeIf { discoverAnnotationProcessorsInCompileClasspath }.orEmpty()

    @get:Internal
    var useBuildCache: Boolean = false

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val source: Collection<File>
        get() {
            val result = HashSet<File>()
            for (root in javaSourceRoots) {
                root.walk().filterTo(result) { it.isJavaFile() }
            }
            return result
        }

    @get:Internal
    protected val javaSourceRoots: Set<File>
        get() = (kotlinCompileTask.sourceRootsContainer.sourceRoots + stubsDir)
            .filterTo(HashSet(), ::isRootAllowed)

    private fun isRootAllowed(file: File): Boolean =
        file.exists() &&
                !FileUtil.isAncestor(destinationDir, file, /* strict = */ false) &&
                !FileUtil.isAncestor(classesDir, file, /* strict = */ false)

    private fun FileCollection?.orEmpty(): FileCollection =
        this ?: project.files()
}