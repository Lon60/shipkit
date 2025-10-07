import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.lombok)
}

dependencies {
    // main
    implementation(libs.springboot.core)
    implementation(libs.springboot.jpa)
    implementation(libs.springboot.actuator)
    implementation(libs.bundles.db.essentials)
    implementation(libs.springDocStarter)

    // internal
    implementation(project(":shipkit-api"))
    runtimeOnly(project(":shipkit-web"))

    // testing
    testImplementation(libs.springboot.test)
}

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("${rootProject.name}-${version}.jar")
}

springBoot {
    mainClass.set("com.shipkit.ShipkitApp")
}

tasks.withType<BootJar> {
    mainClass.set("com.shipkit.ShipkitApp")
}