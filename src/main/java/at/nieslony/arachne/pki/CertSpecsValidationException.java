/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.pki;

/**
 *
 * @author claas
 */
public class CertSpecsValidationException extends PkiException {

    public CertSpecsValidationException(
            CertSpecs.CertSpecType certType,
            CertSpecs.CertSpecKey key,
            String msg
    ) {
        super(certType + "." + key + ": " + msg);
    }

    public CertSpecsValidationException(CertSpecs.CertSpecType certType, String msg) {
        super(certType + " " + msg);
    }

    public CertSpecsValidationException(Exception ex) {
        super(ex);
    }

    public CertSpecsValidationException(String msg) {
        super(msg);
    }
}
