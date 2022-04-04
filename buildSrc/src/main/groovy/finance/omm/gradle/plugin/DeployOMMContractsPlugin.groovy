package finance.omm.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class DeployOMMContractsPlugin implements Plugin<Project> {


    @Override
    void apply(Project target) {
        target.getExtensions().create(ConfigurationExtension.getExtName(), ConfigurationExtension.class, target);
    }
}