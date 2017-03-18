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
public class PermissionDenied extends Exception {

    /**
     * Creates a new instance of <code>PermissionDenied</code> without detail
     * message.
     */
    public PermissionDenied() {
    }

    /**
     * Constructs an instance of <code>PermissionDenied</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public PermissionDenied(String msg) {
        super(msg);
    }
}
