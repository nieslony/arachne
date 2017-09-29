/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.ConfigBuilder;
import at.nieslony.openvpnadmin.TimeUnit;
import at.nieslony.openvpnadmin.beans.FolderFactory;
import at.nieslony.openvpnadmin.beans.ManagementInterface;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.ServerCertificateSettings;
import at.nieslony.openvpnadmin.exceptions.ManagementInterfaceException;
import at.nieslony.utils.pki.CaHelper;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.OperatorCreationException;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class RenewServerCertificate
        //implements ServerCertificateEditor
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{pki}")
    Pki pki;

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    @ManagedProperty(value = "#{managementInterface}")
    ManagementInterface managementInterface;

    public void setManagementInterface(ManagementInterface mi) {
        managementInterface = mi;
    }

    @ManagedProperty(value = "#{serverCertificateSettings}")
    ServerCertificateSettings serverCertificateSettings;

    public void setServerCertificateSettings(ServerCertificateSettings scs) {
        serverCertificateSettings = scs;
    }

    @ManagedProperty(value = "#{configBuilder}")
    ConfigBuilder configBuilder;

    public void setConfigBuilder(ConfigBuilder cb) {
        configBuilder = cb;
    }

    @ManagedProperty(value = "#{folderFactory}")
    FolderFactory folderFactory;

    public void setFolderFactory(FolderFactory ff) {
        folderFactory = ff;
    }

    /**
     * Creates a new instance of RenewServerCertificate
     */
    public RenewServerCertificate() {
    }

    @PostConstruct
    public void init() {
        X509CertificateHolder serverCert = pki.getServerCert();
        X500Name subject = serverCert.getSubject();

        title = CaHelper.getTitle(subject);
        commonName = CaHelper.getCn(subject);
        organization = CaHelper.getOrganization(subject);
        organizationalUnit = CaHelper.getOrganization(subject);
        city = CaHelper.getCity(subject);
        state = CaHelper.getState(subject);
        country = CaHelper.getCountry(subject);

        signatureAlgorithm = serverCert.getSignatureAlgorithm().toString();
        validTime = serverCertificateSettings.getValidTime();
        validTimeUnit = TimeUnit.DAY;
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
    private TimeUnit validTimeUnit;

    public void setTitle(String t) {
        this.title = t;
    }

    public String getTitle() {
        return title;
    }

    public void setCommonName(String cn) {
        commonName = cn;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setOrganization(String o) {
        organization = o;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganizationalUnit(String ou) {
        organizationalUnit = ou;
    }

    public String getOrganizationalUnit() {
        return organizationalUnit;
    }

    public void setCity(String l) {
        city = l;
    }

    public String getCity() {
        return city;
    }

    public void setState(String s) {
        state = s;
    }

    public String getState() {
        return state;
    }

    public void setCountry(String c) {
        country = c;
    }

    public String getCountry() {
        return country;
    }

    public void setValidTime(int time) {
        validTime = time;
    }

    public int getValidTime() {
        return validTime;
    }

    public void setValidTimeUnit(TimeUnit unit) {
        validTimeUnit = unit;
    }

    public TimeUnit getValidTimeUnit() {
        return validTimeUnit;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int ks) {
        keySize = ks;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String sa) {
        signatureAlgorithm = sa;
    }

    public void onRenewServerCertificate() {
        String keyAlgo = CaHelper.getKeyAlgo(signatureAlgorithm);
        X509CertificateHolder oldServerCert = pki.getServerCert();

        logger.info("Starting server certificate renew process...");
        KeyPair keyPair;
        try {
            logger.info("Generation key pair");
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

        Date startDate = new Date();
        long validTimeRange = validTimeUnit.getValue() * validTime;
        Date endDate = new Date(startDate.getTime() + validTimeRange);

        Time startTime = new Time(startDate);
        Time endTime = new Time(endDate);

        X509CertificateHolder cert;
        try {
            logger.info("Creating server certificate");
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
            logger.info("Setting serverccertificate and key");
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

        /*
        try {
            logger.info("Adding old server certificate to CRL");
            pki.addCertificateToCrl(oldServerCert);
        }
        catch (CertIOException | OperatorCreationException ex) {

        }
*/
        String serverConfigFile =
                String.format("%s/clientvpn.conf", folderFactory.getServerConfDir());
        logger.info(String.format(
                "Writing server configuration with new certificate to %s",
                serverConfigFile));
        FileWriter fwr = null;
        try {
            fwr = new FileWriter(serverConfigFile);
            configBuilder.writeUserVpnServerConfig(fwr);
            fwr.flush();
            fwr.close();
        }
        catch (CertificateEncodingException | IOException ex) {
            String msg = String.format("Cannot write server config: %s", ex.getMessage());
            logger.warning(msg);
            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );
        }
        finally {
            try {
                if (fwr != null)
                    fwr.close();
            }
            catch (IOException ex) {
                logger.warning(String.format("Cannot close %s", serverConfigFile));
                return;
            }
        }

        try {
            logger.info("Reloading server configuration");
            managementInterface.reloadConfig();
        }
        catch (IOException | ManagementInterfaceException ex) {
            String msg = String.format("VPN server cannot reload configuration: %s",
                    ex.getMessage());

            logger.warning(msg);

            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );
            return;
        }

        String msg = "Server certificate renew process successfully finished.";
        logger.info(msg);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", msg));
    }

    public TimeUnit[] getValidTimeUnits() {
        return TimeUnit.values();
    }
}
