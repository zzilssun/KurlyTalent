package com.kurly.talent.project

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Compose 설정 For Library
 */
internal fun Project.configureComposeForLibrary() {
    with(pluginManager) {
        apply("org.jetbrains.kotlin.plugin.compose")
    }

    extensions.configure<LibraryExtension> {
        buildFeatures {
            compose = true
        }
    }
}