package eu.asylum.cloud.command.s;

import eu.asylum.cloud.Cloud;
import eu.asylum.cloud.command.Command;
import eu.asylum.common.cloud.enums.ServerType;

public class StopAllCommand extends Command {

    public StopAllCommand() {
        super("stopall");
    }

    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            getLogger().log("Not enough arguments, please specify a server type.");
        } else {
            var forced = false;
            if (args.length > 1) {
                forced = args[1].equalsIgnoreCase("true");
            }
            try {
                ServerType type = ServerType.valueOf(args[0].toUpperCase());
                int counter = 0;
                var servers = Cloud.getInstance().getRepository().getServers(type);
                for (var server : servers) {
                    if (forced) {
                        Cloud.getInstance().forceKill(server);
                    } else {
                        Cloud.getInstance().graciouslyKill(server);
                    }
                }
                getLogger().log("Killed (" + counter + ") Servers. Category <" + type.name() + ">.   FORCED: " + forced);
            } catch (IllegalArgumentException e) {
                getLogger().error("Server type not found.");
            }
        }
    }
}
