plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.shipkit"
version = "0.2.0-SNAPSHOT"
description = "shipkit-web"

dependencies {
    implementation(libs.spring.boot.starter.web)
    testImplementation(libs.spring.boot.starter.test)
    implementation(project(":shipkit-api"))
}
