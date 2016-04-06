package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.compile.AbstractCompile
import java.io.File
import kotlin.reflect.KProperty

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