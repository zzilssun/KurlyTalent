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
    implementation(project(":core"))
    implementation(project(":domain"))

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Location
    implementation(libs.location)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room (Bundle 사용)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Kotlin
    implementation(libs.coroutines.play.services)
}