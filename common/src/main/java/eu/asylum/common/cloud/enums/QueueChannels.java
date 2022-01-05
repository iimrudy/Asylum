package eu.asylum.common.cloud.enums;

import lombok.Getter;

public enum QueueChannels {

    PLAYER_QUEUE_JOIN("asylum:cloud:queue:player:join"),
    PLAYER_QUEUE_LEAVE("asylum:cloud:queue:player:leave"),
    PLAYER_QUEUE_COMPLETE("asylum:cloud:queue:player:complete"),
    PARTY_QUEUE_JOIN("asylum:cloud:queue:party:join"),
    PARTY_QUEUE_LEAVE("asylum:cloud:queue:party:leave"),
    PARTY_QUEUE_COMPLETE("asylum:cloud:queue:party:complete"),
    QUEUE_INFORMATION("asylum:cloud:queue:information"),
    ;

    @Getter
    private final String channel;

    QueueChannels(String chan) {
        this.channel = chan;
    }

}
