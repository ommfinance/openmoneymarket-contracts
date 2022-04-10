package finance.omm.gradle.plugin;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class ConfigContainer {

    private final String name;
    private final Property<String> keystore;
    private final Property<String> password;
    private final Property<String> env;
    private final Property<String> configFile;


    public ConfigContainer(String name, ObjectFactory objectFactory) {
        this.name = name;
        this.keystore = objectFactory.property(String.class);
        this.password = objectFactory.property(String.class);
        this.env = objectFactory.property(String.class).convention("local");
        this.configFile = objectFactory.property(String.class).convention("contracts-sample.json");
    }

    public String getName() {
        return name;
    }


    public Property<String> getKeystore() {
        return keystore;
    }

    public Property<String> getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    public void setKeystore(String keystore) {
        this.keystore.set(keystore);
    }

    public Property<String> getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env.set(env);
    }


    public Property<String> getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile.set(configFile);
    }
}
