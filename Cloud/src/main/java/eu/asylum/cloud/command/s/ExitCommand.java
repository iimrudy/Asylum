package eu.asylum.cloud.command.s;

import eu.asylum.cloud.Cloud;
import eu.asylum.cloud.command.Command;

public class ExitCommand extends Command {

    public ExitCommand() {
        super("exit");
    }

    @Override
    public void onCommand(String[] args) {
        getLogger().log("Good bye!");
        Cloud.getInstance().stopCloud();
    }
}
