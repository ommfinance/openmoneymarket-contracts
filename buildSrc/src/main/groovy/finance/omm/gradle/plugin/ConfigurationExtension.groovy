package finance.omm.gradle.plugin

import finance.omm.gradle.plugin.ConfigContainer
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

public class ConfigurationExtension {

    private static final String EXTENSION_NAME = "deployOMMContracts";

    private final NamedDomainObjectContainer<ConfigContainer> configContainer;

    public static String getExtName() {
        return EXTENSION_NAME;
    }

    public ConfigurationExtension(Project project) {

        configContainer = project.container(ConfigContainer.class,
                name -> new ConfigContainer(name, project.getObjects()));

        configContainer.all(container -> {
            String endpoint = container.getName();
            String capitalizedTarget = endpoint.substring(0, 1).toUpperCase() + endpoint.substring(1);
            String taskName = "deployContractsTo" + capitalizedTarget;
            project.getTasks().register(taskName, DeployOMMContracts.class, task -> {
                task.getKeystore().set(container.getKeystore());
                task.getPassword().set(container.getPassword());
                task.getEnv().set(container.getEnv());
                task.getConfigFile().set(container.getConfigFile());
            });
            var deployTask = project.getTasks().getByName(taskName);
            deployTask.setGroup("Configuration");
            deployTask.setDescription("Deploy OMM Contracts to " + capitalizedTarget + ".");
        });
    }


    public void envs(Action<? super NamedDomainObjectContainer<ConfigContainer>> action) {
        action.execute(configContainer);
    }
}
