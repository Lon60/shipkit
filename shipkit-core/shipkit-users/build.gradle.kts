plugins {
    id("java")
}

group = "com.shipkit"
version = "0.2.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(project(":shipkit-api"))
}

tasks.test {
    useJUnitPlatform()
}