package com.kurly.talent.project

import com.kurly.talent.configureApplicationDefault
import com.kurly.talent.configureLibraryDefault
import org.gradle.api.Project

fun Project.configureApplication(handler: Project.() -> Unit) {
    with(this) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.android")
            apply("com.android.application")
            apply("com.google.devtools.ksp")
            apply("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
        }

        configureKeystore()
        configureCoreDesugaringForApplication()
        configureApplicationDefault()

        commonConfigure()

        handler()
    }
}

fun Project.configureLibrary(handler: Project.() -> Unit) {
    with(this) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.android")
            apply("com.android.library")
            apply("com.google.devtools.ksp")
            apply("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
        }

        configureCoreDesugaringForLibrary()
        configureLibraryDefault()

        commonConfigure()

        handler()
    }
}

private fun Project.commonConfigure() {
    configureDagger()
}