dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation(project(":shipkit-api"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly(project(":shipkit-web"))
}
