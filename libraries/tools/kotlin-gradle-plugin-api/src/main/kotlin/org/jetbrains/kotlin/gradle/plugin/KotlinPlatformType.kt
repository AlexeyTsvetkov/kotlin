/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import java.io.Serializable

enum class KotlinPlatformType: Named, Serializable {
    // naming is not consistent with kotlin style guide
    common,
    jvm,
    js,
    // simply android?
    androidJvm,
    native; // TODO: split native into separate entries here or transform the enum to interface and implement entries in K/N

    override fun toString(): String = name
    override fun getName(): String = name

    companion object {
        // should it actually be public?
        // specify return type
        val attribute = Attribute.of(
            "org.jetbrains.kotlin.platform.type",
            KotlinPlatformType::class.java
        )
    }

    // I'm not sure it belongs here
    class CompatibilityRule : AttributeCompatibilityRule<KotlinPlatformType> {
        override fun execute(details: CompatibilityCheckDetails<KotlinPlatformType>) = with(details) {
            when {
                producerValue == jvm && consumerValue == androidJvm -> compatible()
                producerValue == consumerValue -> compatible()
            }
        }
    }
}