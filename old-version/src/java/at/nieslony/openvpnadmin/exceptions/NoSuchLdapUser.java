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
public class NoSuchLdapUser extends Exception {

    /**
     * Creates a new instance of <code>NoSuchLdapUser</code> without detail
     * message.
     */
    public NoSuchLdapUser() {
    }

    /**
     * Constructs an instance of <code>NoSuchLdapUser</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public NoSuchLdapUser(String msg) {
        super(msg);
    }
}
