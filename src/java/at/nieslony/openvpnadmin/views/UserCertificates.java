/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.FolderFactory;
import at.nieslony.openvpnadmin.beans.Pki;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;

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

    public void removeCertificate() {
        pki.removeUserCert(selectedCert);
    }

    public void onRevokeCertificate() {
        try {
            X509Principal princ = PrincipalUtil.getSubjectX509Principal(selectedCert);
            String cn = princ.getValues(X509Name.CN).get(0).toString();

            Path oldPath = Paths.get(pki.getUserCertFilename(cn));
            Path newPath = Paths.get(folderFactory.getRevokedCertsDir(),
                    String.format("%s.crt", selectedCert.getSerialNumber().toString(16)));

            logger.info(String.format("Revoking cert, moving %s to %s",
                    oldPath.toString(), newPath.toString()));
            Files.move(oldPath, newPath);

            pki.addCertificateToCrl(selectedCert);
            PrintWriter pr = new PrintWriter(pki.getCrlFilename());
            pki.writeCrl(pr);
            pr.close();
        }
        catch (Exception ex) {
            logger.severe(String.format("Cannot revoke certificate: %s", ex.getMessage()));
        }
    }

    public String getCertifivcateStatus(X509Certificate cert) {
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
