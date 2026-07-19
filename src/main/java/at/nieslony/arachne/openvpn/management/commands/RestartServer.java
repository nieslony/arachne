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
public class RestartServer extends Command<Void> {

    public RestartServer(BlockingQueue<Command> queue) {
        super(queue, "signal SIGHUP");
    }

    @Override
    public boolean processResultLine(String line) throws ManagementException {
        return true;
    }

    @Override
    public void processResult() throws ManagementException {
        value.complete(null);
    }

}
