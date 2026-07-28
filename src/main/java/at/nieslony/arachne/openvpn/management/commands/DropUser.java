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
public class DropUser extends SingleLineCommand<String> {

    public DropUser(BlockingQueue<Command> queue, String username) {
        super(queue, "kill " + username);
    }

    @Override
    public void processResult() throws ManagementException {
        value.complete(result);
    }
}
