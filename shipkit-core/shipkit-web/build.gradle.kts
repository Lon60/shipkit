import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.lombok)
}

dependencies {
    // main
    implementation(libs.springboot.web)

    // internal
    implementation(project(":shipkit-api"))
    implementation(project(":shipkit-app"))

    // testing
    testImplementation(libs.springboot.test)
}

tasks.named<BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}