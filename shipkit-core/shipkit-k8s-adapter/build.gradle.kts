plugins {
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.lombok)
}

dependencies {
    implementation(project(":shipkit-api"))
}