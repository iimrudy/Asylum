package eu.asylum.common.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SyncConsoleCommand {

    private final String command;
    private final Callback<String> successCallback;
    private final Callback<Exception> errorCallback;

    public SyncConsoleCommand(String command, Callback<String> onSuccess, Callback<Exception> onError) {
        this.command = command;
        this.successCallback = onSuccess;
        this.errorCallback = onError;
        this.run();
    }

    private void run() {
        try {
            String s;
            String[] command = this.command.split(" ");
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder buffer = new StringBuilder();
            while ((s = reader.readLine()) != null) {
                buffer.append(s);
                buffer.append("\n");
            }
            this.successCallback.call(buffer.toString());
        } catch (Exception e) {
            this.errorCallback.call(e);
        }
    }

    public String getCommand() {
        return this.command;
    }
}
