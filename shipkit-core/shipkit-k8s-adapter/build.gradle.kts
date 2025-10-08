import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.lombok)
}

dependencies {
    implementation(project(":shipkit-api"))
    implementation("io.fabric8:kubernetes-client:6.13.4")
    
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}