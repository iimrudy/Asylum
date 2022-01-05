package eu.asylum.cloud.shell;

import eu.asylum.cloud.Cloud;
import eu.asylum.common.cloud.servers.Server;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedString;

import java.util.List;

public class ServersCompleter implements Completer {

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        for (Server server : Cloud.getInstance().getRepository().getServers()) {
            // this thing should suggest the servers names - don't know how it works don't ask plz
            candidates.add(new Candidate(AttributedString.stripAnsi(server.getName()), server.getName(), null, null, null, null, true));
        }
    }

}
