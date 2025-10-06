dependencies {
    // main
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // internal
    implementation(project(":shipkit-api"))
    runtimeOnly(project(":shipkit-web"))

    // testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
