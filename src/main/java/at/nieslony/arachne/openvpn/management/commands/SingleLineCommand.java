/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.management.commands;

import at.nieslony.arachne.openvpn.management.ManagementException;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
public abstract class SingleLineCommand<T> extends Command<T> {

    String result;

    protected SingleLineCommand(BlockingQueue<Command> queue, String command) {
        super(queue, command);
    }

    @Override
    final public boolean processResultLine(String line) throws ManagementException {
        String[] result = line.split(": ");
        if (result.length != 2) {
            throw new ManagementException("Cannot parse line: " + line);
        }

        if (line.startsWith("SUCCESS:")) {
            log.debug("Single line command return SUCCESS");
            this.result = result[1];
            return true;
        }

        log.debug("Single line command down not return SUCCESS: " + line);
        throw new ManagementException(result[1]);
    }
}
