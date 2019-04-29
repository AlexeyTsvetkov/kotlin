
import org.gradle.jvm.tasks.Jar

description = "Compiler runner + daemon client"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-build-base"))
    compile(project(":compiler:cli-messages"))
    compileOnly(project(":compiler:daemon-common"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

// todo: stop publishing
publish()

val jar: Jar by tasks

runtimeJar(rewriteDepsToShadedCompiler(jar))
sourcesJar()
javadocJar()
