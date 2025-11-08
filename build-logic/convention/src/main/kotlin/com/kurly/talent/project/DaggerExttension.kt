package com.kurly.talent.project

import com.kurly.talent.extensions.findLibrary
import com.kurly.talent.extensions.implementation
import com.kurly.talent.extensions.ksp
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Hilt 설정
 */
fun Project.configureDagger() {
    with(pluginManager) {
        apply("com.google.dagger.hilt.android")
    }

    dependencies {
        implementation(findLibrary("hilt.android"))
        ksp(findLibrary("hilt.android.compiler"))
        implementation(findLibrary("hilt.work"))
        ksp(findLibrary("hilt.work.compiler"))
    }
}