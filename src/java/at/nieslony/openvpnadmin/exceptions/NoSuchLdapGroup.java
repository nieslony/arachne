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
public class NoSuchLdapGroup extends Exception {

    /**
     * Creates a new instance of <code>NoSuchLdapGroup</code> without detail
     * message.
     */
    public NoSuchLdapGroup() {
    }

    /**
     * Constructs an instance of <code>NoSuchLdapGroup</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public NoSuchLdapGroup(String msg) {
        super(msg);
    }
}
