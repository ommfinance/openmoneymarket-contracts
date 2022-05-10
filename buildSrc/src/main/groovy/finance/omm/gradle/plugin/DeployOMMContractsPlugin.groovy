package finance.omm.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class DeployOMMContractsPlugin implements Plugin<Project> {


    @Override
    void apply(Project target) {

        ConfigureOMMEnv configureOMMEnv = target.getTasks().create(ConfigureOMMEnv.getTaskName(), ConfigureOMMEnv.class);
        configureOMMEnv.setGroup("Configuration");
        configureOMMEnv.setDescription("Execute configuration for OMM env");

        target.getExtensions().create(ConfigurationExtension.getExtName(), ConfigurationExtension.class, target);
    }
}