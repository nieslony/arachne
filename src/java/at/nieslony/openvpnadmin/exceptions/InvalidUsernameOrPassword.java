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
public class InvalidUsernameOrPassword extends Exception {

    /**
     * Creates a new instance of <code>InvalidUsernameOrPassword</code> without
     * detail message.
     */
    public InvalidUsernameOrPassword() {
        super("Invalid username or password");
    }

    /**
     * Constructs an instance of <code>InvalidUsernameOrPassword</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public InvalidUsernameOrPassword(String msg) {
        super(msg);
    }
}
