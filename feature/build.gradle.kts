plugins {
    id("kurly.talant.feature")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.kurly.feature"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)

    // ViewModel & Lifecycle (Bundle 사용)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.lifecycle.viewmodel.compose) // ViewModel Compose 추가

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)

    // Google Maps & Location
    implementation(libs.maps)
    implementation(libs.location)
    implementation(libs.coroutines.play.services)
    implementation(libs.maps.compose)
}