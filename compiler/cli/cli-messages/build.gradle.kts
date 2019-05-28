plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.8"

dependencies {
    compile(kotlinStdlib())
    compile(project(":compiler:cli-config-base"))
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }
    compileOnly(commonDep("org.fusesource.jansi", "jansi"))
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