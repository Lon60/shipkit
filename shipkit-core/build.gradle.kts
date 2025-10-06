version = "0.2.0-SNAPSHOT"

plugins {
    java
    id("org.springframework.boot") version "3.5.6" apply false
    id("io.spring.dependency-management") version "1.1.7"
}

subprojects {
    version = rootProject.version

    apply {
        plugin("java")
        plugin("io.spring.dependency-management")
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    repositories {
        mavenCentral()
    }
}