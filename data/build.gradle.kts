plugins {
    id("kurly.talant.library")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.kurly.data"
}

dependencies {
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room (Bundle 사용)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
}