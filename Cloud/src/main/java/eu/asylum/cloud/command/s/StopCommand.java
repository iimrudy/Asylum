package eu.asylum.cloud.command.s;

import eu.asylum.cloud.Cloud;
import eu.asylum.cloud.command.Command;
import eu.asylum.common.cloud.servers.Server;

import java.util.Optional;

public class StopCommand extends Command {

    public StopCommand() {
        super("stop");
    }

    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            getLogger().log("Not enough arguments, please specify a server Name.");
        } else {
            var forced = false;
            if (args.length > 1) {
                forced = args[1].equalsIgnoreCase("true");
            }
            Optional<Server> server = Cloud.getInstance().getRepository().getByName(args[0]);
            if (server.isPresent()) {
                if (forced) {
                    Cloud.getInstance().forceKill(server.get());
                } else {
                    Cloud.getInstance().graciouslyKill(server.get());
                }
                getLogger().log("Server Closed. " + server.get().getName() + "  FORCED: " + forced);
            } else {
                getLogger().error("Server '" + args[0] + "' not found.");
            }
        }
    }
}
