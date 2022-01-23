package eu.asylum.common.cloud.servers;

import lombok.NonNull;

public interface ServerClosableController {

    ServerClosableController DEFAULT = server -> true;

    boolean canClose(@NonNull Server server);

}
