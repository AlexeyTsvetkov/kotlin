
import org.gradle.jvm.tasks.Jar

description = "Compiler runner + daemon client"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-build-common"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:daemon-common"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compileOnly(project(":compiler:util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

val jar: Jar by tasks

runtimeJar(rewriteDepsToShadedCompiler(jar))
sourcesJar()
javadocJar()
