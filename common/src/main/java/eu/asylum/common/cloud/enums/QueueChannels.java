package eu.asylum.common.cloud.enums;

import lombok.Getter;

public enum QueueChannels {

    PLAYER_QUEUE_JOIN("asylum:cloud:queue:player:join"),
    PLAYER_QUEUE_LEAVE("asylum:cloud:queue:player:leave"),
    PARTY_QUEUE_JOIN("asylum:cloud:queue:party:join"),
    PARTY_QUEUE_LEAVE("asylum:cloud:queue:party:leave"),
    QUEUE_CONNECT("asylum:cloud:queue:connect"), // connect the player to the queue
    ;

    @Getter
    private final String channel;

    QueueChannels(String chan) {
        this.channel = chan;
    }

}
