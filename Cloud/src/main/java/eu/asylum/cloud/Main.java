package eu.asylum.cloud;

import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudAdd;
import eu.asylum.common.cloud.servers.Server;
import eu.asylum.common.utils.Constants;

public class Main {

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.setName("UNKNOWN-1");
        server.setPort(25565);
        server.setIp("127.0.0.1");
        server.setServerType(ServerType.LOBBY);
        System.out.println(Constants.get().getGson().toJson(server));
        System.out.println(Constants.get().getGson().toJson(new RedisCloudAdd()));
        Cloud.getInstance();
        new eu.asylum.cloud.shell.CommandHandler().run(); // start the command handler once everything is loaded
    }

}
