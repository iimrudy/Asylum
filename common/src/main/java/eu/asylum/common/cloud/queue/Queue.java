package eu.asylum.common.cloud.queue;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class Queue {

    private UUID id;
    private List<String> players;

}
