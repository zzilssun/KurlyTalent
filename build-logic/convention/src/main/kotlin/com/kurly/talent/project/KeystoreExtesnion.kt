package com.kurly.talent.project

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.FileInputStream
import java.util.Properties

/**
 * Keystore 설정
 */
internal fun Project.configureKeystore() {
    extensions.configure<ApplicationExtension> {
        // Keystore Security
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))

        signingConfigs {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }

        buildTypes {
            named("debug") {
                signingConfig = signingConfigs.getByName("release")
            }
            named("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}