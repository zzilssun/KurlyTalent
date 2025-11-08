package com.kurly.talent

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Application 기본 설정
 */
internal fun Project.configureApplicationDefault() {
    extensions.configure<ApplicationExtension> {
        namespace = "com.kurly.talent"
        compileSdk = 36

        defaultConfig {
            applicationId = "com.kurly.talent"
            minSdk = 24
            targetSdk = 36
            multiDexEnabled = true
        }

        buildFeatures {
            buildConfig = true
        }
    }
}

/**
 * Library 기본 설정
 */
internal fun Project.configureLibraryDefault() {
    extensions.configure<LibraryExtension> {
        compileSdk = 36

        defaultConfig {
            minSdk = 24
        }

        buildFeatures {
            buildConfig = true
        }
    }
}