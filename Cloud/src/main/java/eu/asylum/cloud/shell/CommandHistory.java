package eu.asylum.cloud.shell;


import org.jline.reader.impl.history.DefaultHistory;

import java.time.Instant;

public class CommandHistory extends DefaultHistory {

    private static boolean isComment(String line) {
        return line.startsWith("#");
    }

    @Override
    public void add(Instant time, String line) {
        if (isComment(line)) {
            return;
        }
        super.add(time, line);
    }
}
