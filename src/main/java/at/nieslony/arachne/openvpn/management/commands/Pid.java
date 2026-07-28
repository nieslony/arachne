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
public class Pid extends SingleLineCommand<Integer> {

    public Pid(BlockingQueue<Command> queue) {
        super(queue, "pid");
    }

    @Override
    public void processResult() throws ManagementException {
        log.debug("Parsing " + result);
        String[] tokens = result.split("=");
        if (tokens.length != 2 || !tokens[0].equals("pid")) {
            throw new ManagementException("Cannot parse PID result. Expected: pid=<integer> got: " + result);
        }
        value.complete(Integer.valueOf(tokens[1]));
    }
}
