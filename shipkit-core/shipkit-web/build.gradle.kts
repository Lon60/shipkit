dependencies {
    // main
    implementation(libs.springboot.web)

    // internal
    implementation(project(":shipkit-api"))
    implementation(project(":shipkit-app"))

    // testing
    testImplementation(libs.springboot.test)
}
