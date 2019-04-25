
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:util"))
    implementation(project(":compiler:cli-common"))
    compile(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

