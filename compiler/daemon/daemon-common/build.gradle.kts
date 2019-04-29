
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:util"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:ic:ic-js-base"))
    compile(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

