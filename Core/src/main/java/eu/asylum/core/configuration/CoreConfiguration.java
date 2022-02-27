package eu.asylum.core.configuration;

import eu.asylum.common.configuration.ConfigurationContainer;
import eu.asylum.common.configuration.IConfigurationEnum;
import eu.asylum.core.AsylumCore;
import lombok.Getter;

public enum CoreConfiguration implements IConfigurationEnum {
  CHAT_ENABLED("chat.enabled", true), // is chat enabled ?
  CHAT_FILTER_REPETITION("chat.filter.repetition", true), // anti cheat repetition
  CHAT_FILTER_SWEAR("chat.filter.swear", true), // anti swear
  CHAT_FILTER_ONLY_STAFF("chat.filter.only-staff", false), // only staff can chat
  CHAT_GLOBAL_ENABLED("chat.global.enabled", true), // global chat
  CHAT_GLOBAL_PREFIX("chat.global.prefix", "-"), // global chat prefix
  ;

  @Getter private final String key;
  @Getter private final Object defaultValue;

  CoreConfiguration(String key, Object defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
  }

  @Override
  public final ConfigurationContainer<?> getConfig() {
    return AsylumCore.getInstance().getConfiguration();
  }
}
