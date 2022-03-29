/*
 * Copyright (C) 2018 Claas Nieslony
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.FolderFactory;
import at.nieslony.openvpnadmin.beans.Pki;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Logger;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;

/**
 *
 * @author claas
 */
@ViewScoped
@Named
public class UserCertificates implements Serializable {
    X509CertificateHolder selectedCert;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    Pki pki;
    public void setPki(Pki pki) {
        this.pki = pki;
    }

    @Inject
    private FolderFactory folderFactory;
    public void setFolderFactory(FolderFactory ff) {
        this.folderFactory = ff;
    }

    /**
     * Creates a new instance of UserCertificatesBean
     */
    public UserCertificates() {
    }

    public void setSelectedCert(X509CertificateHolder cert) {
        this.selectedCert = cert;
    }

    public X509CertificateHolder getSelectedCert() {
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
                    selectedCert.getSubject().toString(),
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

    public String getCertificateStatus(X509CertificateHolder cert) {
        if (cert == null)
            return "I";
        if (pki.isCertificateRevoked(cert))
            return "R";

        String status = "V";

        status = cert.isValidOn(new Date()) ? "V" : "E";

        return status;
    }

    public String getSelectedCertPrivKeyDetails() {
        SubjectPublicKeyInfo keyInfo = selectedCert.getSubjectPublicKeyInfo();
        ASN1ObjectIdentifier aid = keyInfo.getAlgorithm().getAlgorithm();

        String keyAlgo = "unknown";
        if (X9ObjectIdentifiers.id_dsa.equals(aid))
            keyAlgo = "DSA";
        else if (X9ObjectIdentifiers.id_ecPublicKey.equals(aid))
            keyAlgo = "ECDSA";
        else if (PKCSObjectIdentifiers.rsaEncryption.equals(aid))
            keyAlgo = "RSA";

        String keySize = "?";

        String keyInfoStr = String.format("%s %s bit", keyAlgo, keySize);
        return keyInfoStr;
    }
}
