package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.compile.AbstractCompile
import java.io.File
import kotlin.reflect.KProperty

/*
 * Adding fake up-to-date check to do something before execution, and before input snapshots are taken.
 * Note, this hack is sensetive to up-to-date checks order, so it should be added before any custom up-to-date checks
 * that could return false.
 */
internal fun AbstractCompile.updateClasspathBeforeTask(newClassPath: ()->FileCollection) {
    // Gradle can take inputs snapshots before task run (normally) or after task run (--rerun-tasks or some up-to-date when returned false).
    // Fake up-to-date check to update classpath dynamically before inputs snapshot is taken.
    // Won't be called in case of some other up-to-date check returned false before this one evaluation
    // Won't be called in case of --rerun-tasks
    var classpathIsUpdated = false
    outputs.upToDateWhen {
        classpath = newClassPath()
        classpathIsUpdated = true
        true
    }

    // in case fake up-to-date check was not called (see comment above)
    doFirst {
        if (!classpathIsUpdated) {
            classpath = newClassPath()
        }
    }
}

internal fun AbstractCompile.appendClasspathDynamically(file: File) {
    doFirst {
        classpath += project.files(file)
    }
    doLast {
        classpath -= project.files(file)
    }
}

// todo: remove when caches could be shared between compileKotlin and compileKotlinAfterJava
internal val AbstractTask.kotlinCacheDirectory: File
        get() {
            val method = this.javaClass.getMethod("getCacheDirectory")
            return method.invoke(this) as File
        }

internal var AbstractTask.anyClassesCompiled by TaskPropertyDelegate<Boolean?>("anyClassesCompiled")
internal var AbstractTask.kotlinDestinationDir by TaskPropertyDelegate<File?>("kotlinDestinationDir")

internal class TaskPropertyDelegate<T>(private val propertyName: String) {
    operator fun getValue(task: Any?, property: KProperty<*>): T? {
        if (task !is AbstractCompile || !task.hasProperty(propertyName)) return null

        @Suppress("UNCHECKED_CAST")
        return task.property(propertyName) as T
    }

    operator fun setValue(task: Any?, property: KProperty<*>, value: T) {
        if (task !is AbstractCompile) return

        task.setProperty(propertyName, value)
    }
}