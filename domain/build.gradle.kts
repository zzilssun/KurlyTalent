plugins {
    id("kurly.talant.library")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.kurly.domain"
}

dependencies {
    implementation(project(":core"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
}