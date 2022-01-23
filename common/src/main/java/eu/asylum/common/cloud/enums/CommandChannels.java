package eu.asylum.common.cloud.enums;

import lombok.Getter;

public enum CommandChannels {

    REDIS_COMMAND_CHANNEL_PATTERN("asylum:cloud:command:*"),
    REDIS_COMMAND_CHANNEL("asylum:cloud:command:"),
    ;

    @Getter
    private final String channel;

    CommandChannels(String chan) {
        this.channel = chan;
    }

}
