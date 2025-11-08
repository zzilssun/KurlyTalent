package com.kurly.talent.project

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.kurly.talent.extensions.coreLibraryDesugaring
import com.kurly.talent.extensions.findLibrary
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Desugaring 설정 For Application
 */
internal fun Project.configureCoreDesugaringForApplication() {
    extensions.configure<ApplicationExtension> {
        compileOptions {
            isCoreLibraryDesugaringEnabled = true
        }
    }

    dependencies {
        // Desugar
        coreLibraryDesugaring(findLibrary("desugar.jdk.libs"))
    }
}

/**
 * Desugaring 설정 For Library
 */
internal fun Project.configureCoreDesugaringForLibrary() {
    extensions.configure<LibraryExtension> {
        compileOptions {
            isCoreLibraryDesugaringEnabled = true
        }
    }

    dependencies {
        // Desugar
        coreLibraryDesugaring(findLibrary("desugar.jdk.libs"))
    }
}