import com.kurly.talent.extensions.implementation

plugins {
    id("kurly.talant.application")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":data"))
    implementation(project(":feature"))
}