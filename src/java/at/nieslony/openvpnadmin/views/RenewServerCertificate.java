/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.ManagementInterface;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.ServerCertificateEditor;
import at.nieslony.openvpnadmin.exceptions.ManagementInterfaceException;
import at.nieslony.utils.pki.CaHelper;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.OperatorCreationException;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class RenewServerCertificate
        implements ServerCertificateEditor
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{pki")
    Pki pki;

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    @ManagedProperty(value = "#{managementInterface")
    ManagementInterface managementInterface;

    public void setManagemtntInterface(ManagementInterface mi) {
        managementInterface = mi;
    }

    /**
     * Creates a new instance of RenewServerCertificate
     */
    public RenewServerCertificate() {
    }

    private String title;
    private String commonName;
    private String organization;
    private String organizationalUnit;
    private String city;
    private String state;
    private String country;
    private String signatureAlgorithm;
    private int keySize;
    private int validTime;
    private String validTimeUnit;

    @Override
    public void setTitle(String t) {
        this.title = t;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void setCommonName(String cn) {
        commonName = cn;
    }

    public String getCommonName() {
        return commonName;
    }

    @Override
    public void setOrganization(String o) {
        organization = o;
    }

    public String getOrganization() {
        return organization;
    }

    @Override
    public void setOrganizationalUnit(String ou) {
        organizationalUnit = ou;
    }

    public String getOrganizationalUnit() {
        return organizationalUnit;
    }

    @Override
    public void setCity(String l) {
        city = l;
    }

    public String getCity() {
        return city;
    }

    @Override
    public void setState(String s) {
        state = s;
    }

    public String getState() {
        return state;
    }

    @Override
    public void setCountry(String c) {
        country = c;
    }

    public String getCountry() {
        return country;
    }

    @Override
    public void setValidTime(Integer time) {
        validTime = time;
    }

    @Override
    public void setValidTimeUnit(String unit) {
        validTimeUnit = unit;
    }

    public void onRenewServerCertificate() {
        String keyAlgo = CaHelper.getKeyAlgo(signatureAlgorithm);
        X509CertificateHolder oldServerCert = pki.getServerCert();

        KeyPair keyPair;
        try {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance(keyAlgo);
            keygen.initialize(keySize, new SecureRandom());
            keyPair = keygen.generateKeyPair();
        }
        catch (NoSuchAlgorithmException ex) {
            String msg = String.format("Cannot create keyPair: %s", ex.getMessage());

            logger.warning(msg);

            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );

            return;
        }

        Time startTime = null;
        Time endTime = null;

        X509CertificateHolder cert;
        try {
            cert = pki.createCertificate(
                keyPair.getPublic(),
                startTime, endTime,
                CaHelper.getSubjectDN(title, commonName, organizationalUnit, organization, city, state, country),
                signatureAlgorithm);
        }
        catch (OperatorCreationException ex) {
            String msg = String.format("Cannot create server sertificate: %s", ex.getMessage());

            logger.warning(msg);

            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );

            return;
        }

        try {
            pki.setServerKeyAndCert(keyPair.getPrivate(), cert);
        }
        catch (ClassNotFoundException | IOException | SQLException ex) {
            String msg = String.format("Cannot save server sertificate and key: %s",
                    ex.getMessage());

            logger.warning(msg);

            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );
            return;
        }

        try {
            pki.addCertificateToCrl(oldServerCert);
        }
        catch (CertIOException | OperatorCreationException ex) {
            
        }

        try {
            managementInterface.reloadConfig();
        }
        catch (IOException | ManagementInterfaceException ex) {
            String msg = String.format("VPÖN server cannot reload configuration: %s",
                    ex.getMessage());

            logger.warning(msg);

            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );
            return;
        }
    }
}
