/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.exceptions;

/**
 *
 * @author claas
 */
public class ManagementInterfaceException extends Exception {

    /**
     * Creates a new instance of <code>ManagementInterfaceException</code>
     * without detail message.
     */
    public ManagementInterfaceException() {
    }

    /**
     * Constructs an instance of <code>ManagementInterfaceException</code> with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public ManagementInterfaceException(String msg) {
        super(msg);
    }

    public ManagementInterfaceException(String command, String msg) {
        super(String.format("Error sending command %s: %s",
                command, msg));
    }
}
