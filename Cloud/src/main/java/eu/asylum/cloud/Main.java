package eu.asylum.cloud;

public class Main {

  public static void main(String[] args) throws Exception {
    Cloud.getInstance();
    new eu.asylum.cloud.shell.CommandHandler()
        .run(); // start the command handler once everything is loaded
  }
}
