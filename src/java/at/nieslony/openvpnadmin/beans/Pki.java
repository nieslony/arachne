/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;


import at.nieslony.utils.pki.CertificateAuthority;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.security.auth.x500.X500Principal;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class Pki extends CertificateAuthority implements Serializable {
    private static final long serialVersionUID = 1237L;

    private static final String FN_CA_CRT = "/ca.crt";
    private static final String FN_CA_KEY = "/ca.key";
    private static final String FN_SERVER_CRT = "/server.crt";
    private static final String FN_SERVER_KEY = "/server.key";
    private static final String FN_DH = "/dh.pem";
    private static final String FN_CRL = "/ca.crl";

    private X509Certificate serverCert;
    private PrivateKey serverKey;

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;

    @ManagedProperty(value = "#{clientCertificateSettings}")
    private ClientCertificateSettings clientCertificateSettings;

    public void setClientCertificateSettings(ClientCertificateSettings ccs) {
        clientCertificateSettings = ccs;
    }

    public String getDhFilename() {
        return folderFactory.getPkiDir() + FN_DH;
    }

    public String getCaCertFilename() {
        return folderFactory.getPkiDir() + FN_CA_CRT;
    }

    public String getServerCertFilename() {
        return folderFactory.getPkiDir() + FN_SERVER_CRT;
    }

    public String getServerKeyFilename() {
        return folderFactory.getPkiDir() + FN_SERVER_KEY;
    }

    public String getCrlFilename() {
        return folderFactory.getPkiDir() + FN_CRL;
    }

    public List<X509Certificate> getAllUserCerts() throws IOException, CertificateException  {
        List<X509Certificate> certs = new LinkedList<>();

        File dir = new File(folderFactory.getUserCertsDir());
        for (String certFN: dir.list(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.matches(".*\\.crt");
                    }
                })
                ) {
            X509Certificate cert;
            String fn = dir.getPath() + "/" + certFN;
            logger.info(String.format("Reading cert %s", fn));
            FileInputStream fis = new FileInputStream(fn);
            cert = readCertificate(fis);
            fis.close();

            certs.add(cert);
        }

        return certs;
    }

    public void setFolderFactory(FolderFactory ff) {
        this.folderFactory = ff;
    }

    public String getUserCertFilename(String username) {
        StringBuilder sb = new StringBuilder();

        sb.append(folderFactory.getUserCertsDir());
        sb.append("/");
        sb.append(username);
        sb.append(".crt");

        return sb.toString();
    }

    public String getUserKeyFilename(String username) {
        StringBuilder sb = new StringBuilder();

        sb.append(folderFactory.getUserCertsDir());
        sb.append("/");
        sb.append(username);
        sb.append(".key");

        return sb.toString();
    }

    public Pki() {
    }

    private void loadCaCert() throws Exception {
        FileInputStream fis;
        logger.info("Loading CA certificate...");
        fis = new FileInputStream(folderFactory.getPkiDir() + FN_CA_CRT);
        setCaCert(readCertificate(fis));
        fis.close();
    }

    private void loadCaKey() throws Exception {
        FileInputStream fis;
        logger.info("Loading CA key...");
        fis = new FileInputStream(folderFactory.getPkiDir() + FN_CA_KEY);
        setCaKey(readPrivateKey(fis));
        fis.close();
    }

    private void loadCrl()
            throws CRLException, IOException, CertificateException,
            NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, SignatureException
    {
        FileInputStream fis;
        String fn = folderFactory.getPkiDir() + FN_CRL;
        logger.info(String.format("Loading CRL from %s...", fn));

        try {
            fis = new FileInputStream(fn);
            setCrl(readCrl(fis));
        }
        catch (FileNotFoundException ex) {
            logger.info("CRL not found, creating new one");
            createCrl();
            logger.info("Writing CRL");
            try (PrintWriter pw = new PrintWriter(fn)) {
                writeCrl(pw);
            }
        }
    }

    private void loadServerCert() throws Exception {
        FileInputStream fis;
        logger.info("Loading server certificate...");
        fis = new FileInputStream(folderFactory.getPkiDir() + FN_SERVER_CRT);
        serverCert = readCertificate(fis);
        fis.close();
    }

    private void loadServerKey() throws Exception {
        FileInputStream fis;
        logger.info("Loading server key...");
        fis = new FileInputStream(folderFactory.getPkiDir() + FN_SERVER_KEY);
        serverKey = readPrivateKey(fis);
        fis.close();
    }

    private X509Certificate loadUserCert(String username) throws Exception {
        X509Certificate cert;

        FileInputStream fis = new FileInputStream(getUserCertFilename(username));
        cert = readCertificate(fis);
        return cert;
    }

    private PrivateKey loadUserKey(String username) throws Exception {
        PrivateKey key;

        FileInputStream fis = new FileInputStream(getUserKeyFilename(username));
        key  = readPrivateKey(fis);
        return key;
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing PKI");

        setCaDir(folderFactory.getPkiDir());

        try {
            loadCaCert();
            loadCaKey();
            loadServerCert();
            loadServerKey();
            loadCrl();
        }
        catch (FileNotFoundException ex) {
            logger.severe(String.format("Cannot init Pki: %s", ex.toString()));
        }
        catch (Exception ex) {
            logger.severe(String.format("Cannot init Pki: %s", ex.toString()));
        }
    }


    public void setServerCert(X509Certificate cert) {
        this.serverCert = cert;
    }

    public void setServerKey(PrivateKey key) {
        this.serverKey = key;
    }

    public X509Certificate getServerCert() {
        return serverCert;
    }

    private void createUserCertAndKey(String username)
            throws GeneralSecurityException, IOException {
        X509Certificate cert;

        if (clientCertificateSettings == null) {
            logger.severe("clientCertificateSettings must not be null");
        }

        logger.info("Creating new key pair for user" + username);
        KeyPairGenerator keygen;
        KeyPair certKey;

        try {
            keygen = KeyPairGenerator.getInstance(clientCertificateSettings.getKeyAlgorithm());
            keygen.initialize(clientCertificateSettings.getKeySize(), new SecureRandom());
            certKey = keygen.generateKeyPair();
        }
        catch (NoSuchAlgorithmException ex) {
            logger.severe("Cannot create key pair" + ex.getMessage());
            throw ex;
        }

        logger.info("Preparing certificate creation for user " + username);
        StringWriter sw = new StringWriter();
        sw.append("CN=" + username);
        if (!clientCertificateSettings.getOPrganizationalUnit().isEmpty())
            sw.append(", OU=" + clientCertificateSettings.getOPrganizationalUnit());
        if (!clientCertificateSettings.getOrganization().isEmpty())
            sw.append(", O=" + clientCertificateSettings.getOrganization());
        if (!clientCertificateSettings.getCity().isEmpty())
            sw.append(", L=" + clientCertificateSettings.getCity());
        if (!clientCertificateSettings.getState().isEmpty())
            sw.append(", ST=" + clientCertificateSettings.getState());
        if (!clientCertificateSettings.getCountry().isEmpty())
            sw.append(", C=" + clientCertificateSettings.getCountry());

        logger.info("------------ Subject:" + sw.toString());

        X500Principal subject = new X500Principal(sw.toString());
        Date fromDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        if (clientCertificateSettings.getValidTimeUnit().equals("days")) {
            cal.add(Calendar.DAY_OF_MONTH, clientCertificateSettings.getValidTime());
        }
        else if (clientCertificateSettings.getValidTimeUnit().equals("months")) {
            cal.add(Calendar.MONTH, clientCertificateSettings.getValidTime());
        }
        else if (clientCertificateSettings.getValidTimeUnit().equals("years")) {
            cal.add(Calendar.YEAR, clientCertificateSettings.getValidTime());
        }
        Date toDate = cal.getTime();

        logger.info(String.format(
                "Creating certificate:\n" +
                "  subject: %s\n" +
                "  valid from: %s\n" +
                "  valid to: %s" +
                "  signature algorothm: %s",
                subject, fromDate.toString(), toDate.toString(),
                clientCertificateSettings.getSignatureAlgorith()));
        cert = createCertificate(certKey.getPublic(), fromDate, toDate, subject,
                clientCertificateSettings.getSignatureAlgorith());

        if (cert == null) {
            logger.severe("Something went wrong. certificate is null");
        }

        PrintWriter pw;
        pw = new PrintWriter(new File(getUserCertFilename(username)));
        writeCertificate(cert, pw);
        pw.close();

        pw = new PrintWriter(new File(getUserKeyFilename(username)));
        writePrivateKey(certKey.getPrivate(), pw);
        pw.close();
    }

    public X509Certificate getUserCert(String username) {
        X509Certificate cert = null;

        try {
            cert = loadUserCert(username);
        }
        catch (FileNotFoundException ex) {
            logger.warning(String.format("There's no certificate for user %s, creating new certificate and private key",
                    username));
            try {
                createUserCertAndKey(username);
                cert = loadUserCert(username);
            }
            catch (Exception ex2) {
                logger.severe(String.format("Error creating and writing user certificate for user %s\n%s,",
                        username, ex.getMessage()));
            }
        }
        catch (Exception ex) {
            logger.severe(String.format("Error loading user certificate for user %s: %s",
                    username, ex.getMessage()));
        }

        return cert;
    }

    public PrivateKey getUserKey(String username) {
        PrivateKey key = null;

        try {
            key = loadUserKey(username);
        }
        catch (FileNotFoundException ex) {
            logger.severe(String.format("There's no private key for user %s, creating new certificate and private key",
                    username));
            try {
                createUserCertAndKey(username);
                key = loadUserKey(username);
            }
            catch (Exception ex2) {
                logger.severe(String.format(
                        "Error creating and writing user certificate for user %s: %s",
                        username, ex.getMessage()));
            }
        }
        catch (Exception ex) {
            logger.severe(String.format("Error loading private key for user %s: %s",
                    username, ex.getMessage()));
        }

        return key;
    }



    public boolean isValid() {
        try {
            if (getCaCert() == null) {
                logger.warning("CA cert is invalid, trying reload.");
                loadCaCert();
                if (getCaCert() == null) {
                    logger.severe("CA cert is still invalid. giving up.");
                    return false;
                }
            }
            if (getCaKey() == null) {
                logger.warning("CA key is invalid, trying reload.");
                loadCaKey();
                if (getCaKey() == null) {
                    logger.severe("CA key is still invalid. giving up.");
                    return false;
                }
            }
            if (serverCert == null) {
                logger.warning("Server cert is invalid, trying reload.");
                loadServerCert();
                if (serverCert== null) {
                    logger.severe("Server cert is still invalid. giving up.");
                    return false;
                }
            }
            if (serverKey == null) {
                logger.warning("Server key is invalid, trying reload.");
                loadServerKey();
                if (serverKey == null) {
                    logger.severe("Server key is still invalid. giving up.");
                    return false;
                }
            }
            return true;
        }
        catch (Exception ex) {
            logger.severe(String.format("PKI is invalid: %s", ex.getMessage()));
        }
        return false;
    }

    public void removeUserCert(X509Certificate cert) {
        if (cert == null)
            logger.warning("Cannot remove a null certificate");
        Principal princ = cert.getSubjectDN();
        String princLeaf = princ.getName().split(",")[0];
        if (princLeaf.split("=")[0].equalsIgnoreCase("cn")) {
            String username = princLeaf.split("=")[1];
            String fn = getUserCertFilename(username);
            File f = new File(fn);
            logger.info(String.format("Removing %s", fn));
            f.delete();
        }
        else {
            logger.severe(String.format("Principal doesn't contain common name: %s",
                    princLeaf));
        }
    }
}
