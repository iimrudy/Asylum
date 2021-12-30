package eu.asylum.common.cloud.redis;

import lombok.Getter;

public enum CloudChannels {

    SERVER_ADD("asylum:cloudL:server:add"),
    SERVER_DELETE("asylum:cloud:server:delete"),
    SERVER_UPDATE("asylum:cloud:server:update"),
    SERVER_SHUTDOWN("asylum:cloud:server:shutdown"),
    MESSAGE("asylum:cloud:message"),
    SYNC("asylum:cloud:sync"),
    ;

    @Getter
    private final String channel;

    CloudChannels(String chan) {
        this.channel = chan;
    }

}
