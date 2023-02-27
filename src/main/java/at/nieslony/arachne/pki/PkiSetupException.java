/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

/**
 *
 * @author claas
 */
public class PkiSetupException extends Exception {

    public PkiSetupException(Pki.CertSpecType certType, Pki.CertSpecKey key, String msg) {
        super(certType + "." + key + ": " + msg);
    }

    public PkiSetupException(Pki.CertSpecType certType, String msg) {
        super(certType + " " + msg);
    }

    public PkiSetupException(Exception ex) {
        super(ex);
    }
}
