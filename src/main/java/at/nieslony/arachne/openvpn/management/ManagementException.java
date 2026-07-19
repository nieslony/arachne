/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.management;

/**
 *
 * @author claas
 */
public class ManagementException extends Exception {

    public ManagementException(String what) {
        super(what);
    }

    public ManagementException(String what, Throwable cause) {
        super(what, cause);
    }
}
