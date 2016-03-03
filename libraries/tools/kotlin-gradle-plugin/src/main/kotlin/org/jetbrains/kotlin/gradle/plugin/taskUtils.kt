package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Task

inline
fun <reified T> Task.getProperty(propertyName: String): T? =
        if (hasProperty(propertyName)) property(propertyName) as? T else null