/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.FolderFactory;
import at.nieslony.openvpnadmin.beans.Pki;
import java.io.Serializable;
import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class UserCertificates implements Serializable {
    X509Certificate selectedCert;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{pki}")
    Pki pki;
    public void setPki(Pki pki) {
        this.pki = pki;
    }

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;
    public void setFolderFactory(FolderFactory ff) {
        this.folderFactory = ff;
    }

    /**
     * Creates a new instance of UserCertificatesBean
     */
    public UserCertificates() {
    }

    public void setSelectedCert(X509Certificate cert) {
        this.selectedCert = cert;
    }

    public X509Certificate getSelectedCert() {
        return selectedCert;
    }

    public void removeCertificate()
    {
        if (selectedCert == null) {
            String msg = "No certificate selected";
            logger.info(msg);
            return;
        }

        try {
            pki.removeKeyAndCert(selectedCert);
        }
        catch (ClassNotFoundException | SQLException ex) {
            String msgStr = String.format("Cannot remove certificate %s: %s",
                    selectedCert.getSubjectDN().toString(),
                    ex.getMessage());
            FacesContext.getCurrentInstance().addMessage(
                    null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Warning", msgStr));
        }
    }

    public void onRevokeCertificate() {
        try {
            pki.revoveCert(selectedCert);
        }
        catch (Exception ex) {
            logger.severe(String.format("Cannot revoke certificate: %s", ex.getMessage()));
        }
    }

    public String getCertifivcateStatus(X509Certificate cert) {
        if (cert == null)
            return "I";
        if (pki.isCertificateRevoked(cert))
            return "R";

        String status = "V";
        try {
            cert.checkValidity();
        }
        catch (CertificateExpiredException ex) {
            status = "E";
        }
        catch (CertificateNotYetValidException ex) {
            status = "N";
        }

        return status;
    }

    public String getSelectedCertPrivKeyDetails() {
        String keyAlgo = selectedCert.getPublicKey().getAlgorithm();

        PublicKey publicKey = selectedCert.getPublicKey();
        String keySize = "unknown";
        if (publicKey instanceof RSAPublicKey) {
		keySize = String.valueOf(
                        ((RSAPublicKey)publicKey).getModulus().bitLength()
                );
	}
        else if (publicKey instanceof DSAPublicKey) {
		keySize = String.valueOf(
                        ((DSAPublicKey)publicKey).getParams().getP().bitLength()
                );
	}

        String keyInfo = String.format("%s %s bit", keyAlgo, keySize);
        return keyInfo;
    }
}
