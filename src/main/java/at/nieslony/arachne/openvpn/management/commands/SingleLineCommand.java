/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.management.commands;

import at.nieslony.arachne.openvpn.management.ManagementException;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author claas
 */
public abstract class SingleLineCommand<T> extends Command<T> {

    String result;

    protected SingleLineCommand(BlockingQueue<Command> queue, String command) {
        super(queue, command);
    }

    @Override
    public boolean processResultLine(String line) throws ManagementException {
        String[] result = line.split(": ");
        if (result.length != 2) {
            throw new ManagementException("Cannot parse line: " + line);
        }

        if (line.startsWith("SUCCESS:")) {
            this.result = result[1];
            return true;
        }

        throw new ManagementException(result[1]);
    }
}
