
description = "Kotlin Build Base"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
