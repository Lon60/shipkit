import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.shipkit"
version = "0.2.0-SNAPSHOT"
description = "shipkit-app"

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)
    developmentOnly(libs.spring.boot.devtools)
    runtimeOnly(libs.postgres)
    testImplementation(libs.spring.boot.starter.test)
    implementation(project(":shipkit-api"))
}

tasks.named<BootJar>("bootJar") {
    enabled = false
}
tasks.named<Jar>("jar") {
    enabled = true
}
