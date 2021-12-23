package eu.asylum.common.configuration;

import lombok.Getter;

public enum AsylumConfiguration implements IConfigurationEnum {

    MONGODB_URI("mongo_uri", "mongodb://localhost:27017"),
    REDIS_URI("redis_uri", "redis://127.0.0.1:6379/0"),
    ;


    @Getter
    private final String key;
    @Getter
    private final Object defaultValue;

    AsylumConfiguration(String key, Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }


}

