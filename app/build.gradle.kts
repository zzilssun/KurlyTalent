import com.kurly.talent.extensions.implementation

plugins {
    id("kurly.talant.application")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":feature"))
}