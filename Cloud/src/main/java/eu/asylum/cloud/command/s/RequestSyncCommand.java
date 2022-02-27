package eu.asylum.cloud.command.s;

import eu.asylum.cloud.Cloud;
import eu.asylum.cloud.command.Command;

public class RequestSyncCommand extends Command {
  public RequestSyncCommand() {
    super("sync");
  }

  @Override
  public void onCommand(String[] args) {
    getLogger().log("Sync requested...");
    Cloud.getInstance().requestSync();
  }
}
