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
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // WorkManager
    implementation(libs.work.runtime.ktx)
}