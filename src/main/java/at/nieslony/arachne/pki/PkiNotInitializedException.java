/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

/**
 *
 * @author claas
 */
public class PkiNotInitializedException extends PkiException {

    public PkiNotInitializedException(String msg) {
        super("msg: " + "pki not yes initialized");
    }
}
