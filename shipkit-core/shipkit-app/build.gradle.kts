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
