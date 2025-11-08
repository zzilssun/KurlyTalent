import com.kurly.talent.project.configureComposeForLibrary
import com.kurly.talent.project.configureLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configureLibrary {
            configureComposeForLibrary()
        }
    }
}