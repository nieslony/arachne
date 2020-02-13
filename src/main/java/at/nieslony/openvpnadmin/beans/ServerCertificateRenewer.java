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

package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.ConfigBuilder;
import at.nieslony.openvpnadmin.TimeUnit;
import at.nieslony.openvpnadmin.exceptions.ManagementInterfaceException;
import at.nieslony.utils.pki.CaHelper;
import at.nieslony.utils.pki.CertificateAuthority;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.AlgorithmNameFinder;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;
import org.bouncycastle.operator.OperatorCreationException;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class ServerCertificateRenewer
        implements Serializable
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

    private List<SelectItem> signatureAlgorithms;

    public List<SelectItem> getSignatureAlgorithms() {
        return signatureAlgorithms;
    }

    /**
     * Creates a new instance of ServerCertificateRenewer
     */
    public ServerCertificateRenewer() {
    }

    @PostConstruct
    public void init() {
        signatureAlgorithms = new ArrayList<>();
        for (CertificateAuthority.KeySignAlgo ksa : CertificateAuthority.getKeySignAlgos()) {
            String keyAlgo = ksa.keyAlgo;
            SelectItemGroup group = new SelectItemGroup(keyAlgo);
            SelectItem[] items = new SelectItem[ksa.signatureAlgos.length];
            for (int i = 0; i < ksa.signatureAlgos.length; i++) {
                String label = ksa.signatureAlgos[i] + " with " + keyAlgo;
                String value = ksa.signatureAlgos[i] + "with" + keyAlgo;
                value = value.toUpperCase();
                items[i] = new SelectItem(value, label);
            }
            group.setSelectItems(items);
            signatureAlgorithms.add(group);
        }
    }

    public boolean setDefaultValues(ServerCertificateEditor editor) {
        X509CertificateHolder serverCert = pki.getServerCert();
        if (serverCert == null)
            return false;
        X500Name subject = serverCert.getSubject();

        editor.setTitle(CaHelper.getTitle(subject));
        editor.setCommonName(CaHelper.getCn(subject));
        editor.setOrganization(CaHelper.getOrganization(subject));
        editor.setOrganizationalUnit(CaHelper.getOrganization(subject));
        editor.setCity(CaHelper.getCity(subject));
        editor.setState(CaHelper.getState(subject));
        editor.setCountry(CaHelper.getCountry(subject));

        AlgorithmNameFinder algoFinder = new DefaultAlgorithmNameFinder();
        String signatureAlgorithm = algoFinder.getAlgorithmName(serverCert.getSignatureAlgorithm());
        editor.setSignatureAlgorithm(signatureAlgorithm);
        editor.setValidTime(365);
        editor.setValidTimeUnit(TimeUnit.DAY);

        int keySize = 0;
        try {
            keySize = CaHelper.getKeySize(serverCert.getSubjectPublicKeyInfo());
        }
        catch (IOException ex) {
            logger.warning(String.format("Cannot get key size: %s", ex.getMessage()));
        }
        logger.info(String.format("Key size: %d", keySize));
        editor.setKeySize(keySize);

        return true;
    }

    public void renewServerCertificate(ServerCertificateEditor editor) {
        String keyAlgo = CaHelper.getKeyAlgo(editor.getSignatureAlgorithm());
        X509CertificateHolder oldServerCert = pki.getServerCert();

        logger.info("Starting server certificate renew process...");
        KeyPair keyPair;
        try {
            logger.info(String.format("Generation %s key pair", keyAlgo));
            KeyPairGenerator keygen = KeyPairGenerator.getInstance(keyAlgo, BouncyCastleProvider.PROVIDER_NAME);
            keygen.initialize(editor.getKeySize(), new SecureRandom());
            keyPair = keygen.generateKeyPair();
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            String msg = String.format("Cannot create keyPair: %s", ex.getMessage());

            logger.warning(msg);

            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx != null)
                ctx.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Error", msg)
                        );

            return;
        }

        Date startDate = new Date();
        long validTimeRange = (long) editor.getValidTimeUnit().getValue() * editor.getValidTime();
        Date endDate = new Date(startDate.getTime() + validTimeRange);

        Time startTime = new Time(startDate);
        Time endTime = new Time(endDate);

        X509CertificateHolder cert;
        try {
            logger.info("Creating server certificate");
            cert = pki.createCertificate(
                keyPair.getPublic(),
                startTime, endTime,
                editor.getSubjectDn(),
                editor.getSignatureAlgorithm());
        }
        catch (OperatorCreationException ex) {
            String msg = String.format("Cannot create server sertificate: %s", ex.getMessage());

            logger.warning(msg);

            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx != null)
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
            if (ctx != null)
                ctx.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Error", msg)
                        );

            return;
        }

        PrintWriter wr = null;
        try {
            logger.info("Adding old server certificate to CRL");
            pki.revoveCert(oldServerCert);

            wr = new PrintWriter(new FileWriter(pki.getCrlFilename()));
            pki.writeCrl(wr);
        }
        catch (IOException | OperatorCreationException | CRLException | ClassNotFoundException | SQLException ex) {
            String msg = String.format("Cannot add old certificate to CRL: %s", ex.getMessage());
            logger.warning(msg);
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx != null)
                ctx.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Error", msg)
                        );
            return;
        }
        finally {
            if (wr != null)
                wr.close();
        }

        String serverConfigFile = folderFactory.getUserVpnFileName();
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
            if (ctx != null)
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
            if (ctx != null)
                ctx.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Error", msg)
                        );
            return;
        }

        String msg = "Server certificate renew process successfully finished.";
        logger.info(msg);
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null)
            ctx.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", msg));
    }
}

