package at.nieslony.arachne.openvpn.management.commands;

import at.nieslony.arachne.openvpn.management.ManagementException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
abstract public class MultiLineCommand<T> extends Command<T> {

    protected List<String> lines = new ArrayList<>();

    protected MultiLineCommand(BlockingQueue<Command> queue, String command) {
        super(queue, command);
    }

    @Override
    final public boolean processResultLine(String line) throws ManagementException {
        if (line.startsWith("ERROR: ")) {
            log.error("Got error: " + line);
            throw new ManagementException(line);
        }

        if (line.equals("END")) {
            log.debug("Got END");
            return true;
        }

        log.debug("Add line " + line);
        lines.add(line);
        return false;
    }
}
