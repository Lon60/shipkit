version = "0.2.0-SNAPSHOT"

plugins {
    java
}

repositories {
    mavenCentral()
}

subprojects {
    version = rootProject.version
    apply {
        plugin("java")
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType(Test::class.java).configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

tasks.register("bootJar") {
    dependsOn(":shipkit-app:bootJar")
    dependsOn("assembleBootJar")
    group = "build"
    description = "Delegates to shipkit-app's bootJar"
}

tasks.register<Copy>("assembleBootJar") {
    dependsOn(":shipkit-app:bootJar")
    from(project(":shipkit-app").layout.buildDirectory.dir("libs"))
    into(layout.buildDirectory.dir("libs"))
}