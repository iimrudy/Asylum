package eu.asylum.cloud.command.s;

import eu.asylum.cloud.Cloud;
import eu.asylum.cloud.command.Command;
import eu.asylum.common.cloud.servers.Server;
import java.util.Optional;

public class PingCommand extends Command {

  public PingCommand() {
    super("ping");
  }

  @Override
  public void onCommand(String[] args) {
    if (args.length == 0) {
      getLogger().log("Not enough arguments, please specify a server Name.");
    } else {
      Optional<Server> server = Cloud.getInstance().getRepository().getByName(args[0]);
      if (server.isPresent()) {
        long start = System.currentTimeMillis();
        getLogger().log("Pinging " + server.get().getName() + "...");
        if (server.get().getPinger().ping()) {
          getLogger().log("Online Players (" + server.get().getPinger().getPlayersOnline() + ").");
          getLogger().log("Ping " + (System.currentTimeMillis() - start) + "ms.");
        } else {
          getLogger().log(server.get().getName() + " can't be reached.");
        }

      } else {
        getLogger().error("Server '" + args[0] + "' not found.");
      }
    }
  }
}
