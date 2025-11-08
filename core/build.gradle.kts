plugins {
    id("kurly.talant.library")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.kerly.core"
}

dependencies {
    implementation(libs.bundles.lifecycle)
}