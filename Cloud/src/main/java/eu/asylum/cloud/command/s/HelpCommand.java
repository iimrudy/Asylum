package eu.asylum.cloud.command.s;

import eu.asylum.cloud.command.Command;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "?");
    }

    @Override
    public void onCommand(String[] args) {
        getLogger().log("'?' for help");
        getLogger().log("'host <ServerType>' for hosting a server");
        getLogger().log("'stop <ServerName>' for stopping a server");
        getLogger().log("'stopall <ServerType>' for stopping an entire category");
        getLogger().log("'info [ServerType]' get servers info");
        getLogger().log("\n<> needed, [] optional");
    }
}
