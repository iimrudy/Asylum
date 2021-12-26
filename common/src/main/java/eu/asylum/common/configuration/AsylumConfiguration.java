package eu.asylum.common.configuration;

import lombok.Getter;

public enum AsylumConfiguration implements IConfigurationEnum {


    MONGODB_URI("mongo_uri", "mongodb://localhost:27017"),
    REDIS_URI("redis_uri", "redis://127.0.0.1:6379/0"),
    ;

    private static ConfigurationContainer<?> configurationContainer;

    @Getter
    private final String key;
    @Getter
    private final Object defaultValue;

    AsylumConfiguration(String key, Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public static void setConfigurationContainer(ConfigurationContainer<?> configurationContainer) {
        AsylumConfiguration.configurationContainer = configurationContainer;
    }

    @Override
    public ConfigurationContainer<?> getConfig() {
        return configurationContainer;
    }
}

