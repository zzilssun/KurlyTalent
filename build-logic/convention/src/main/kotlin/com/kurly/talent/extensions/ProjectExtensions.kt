package com.kurly.talent.extensions

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal fun Project.findLibrary(alias: String) = libs.findLibrary(alias).get()

internal fun Project.findBundle(alias: String) = libs.findBundle(alias).get()

private val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")