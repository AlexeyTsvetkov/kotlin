plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.8"

dependencies {
    compileOnly(kotlinStdlib())
    compile(project(":core:util.runtime"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        languageVersion = "1.2"
        apiVersion = "1.2"
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }
}