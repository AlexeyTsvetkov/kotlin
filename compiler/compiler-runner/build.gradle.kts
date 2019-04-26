
import org.gradle.jvm.tasks.Jar

description = "Compiler runner + daemon client"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-build-common"))
    compile(project(":compiler:cli-messages"))
    compileOnly(project(":compiler:daemon-common"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
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
