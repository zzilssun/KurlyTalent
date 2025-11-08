import com.android.build.api.dsl.ApplicationExtension
import com.kurly.talent.project.configureApplication
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configureApplication {
            extensions.configure<ApplicationExtension> {
                defaultConfig {
                    versionName = "1.0.0"
                    versionCode = 1
                }

                buildTypes {
                    named("release") {
                        proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
                        isMinifyEnabled = true
                        isShrinkResources = true
                    }
                }
            }
        }
    }
}