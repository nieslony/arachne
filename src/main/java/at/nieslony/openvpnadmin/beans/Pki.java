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


import at.nieslony.utils.DbUtils;
import at.nieslony.utils.pki.CertificateAuthority;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.operator.AlgorithmNameFinder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.postgresql.util.PSQLException;

/**
 *
 * @author claas
 */
@ManagedBean(name = "pki")
@ApplicationScoped
public class Pki
        extends CertificateAuthority
        implements Serializable
{
    transient private X509CertificateHolder serverCert;
    transient private PrivateKey serverKey;

    private static final String FN_DH = "/dh.pem";
    private static final String FN_CRL = "/ca.crl";

    public enum CertType {
        CA,
        SERVER,
        CLIENT
    }

    public class KeyAndCert {
        private PrivateKey key;
        private X509CertificateHolder cert;

        public KeyAndCert(PrivateKey key, X509CertificateHolder cert) {
            this.key = key;
            this.cert = cert;
        }

        public X509CertificateHolder getCert() {
            return cert;
        }

        public PrivateKey getKey() {
            return key;
        }
    }

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;
    public void setFolderFactory(FolderFactory ff) {
        this.folderFactory = ff;
    }

    @ManagedProperty(value = "#{clientCertificateSettings}")
    private ClientCertificateSettings clientCertificateSettings;
    public void setClientCertificateSettings(ClientCertificateSettings ccs) {
        clientCertificateSettings = ccs;
    }

    @ManagedProperty(value = "#{databaseSettings}")
    private DatabaseSettings databaseSettings;
    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }


    @PostConstruct
    @Override
    public void init() {
        logger.info("Initializing PKI");

        super.init();

        setCaDir(folderFactory.getPkiDir());

        try {
            logger.info("Loading CA key and certificate");
            loadCaKeyAndCert();
            logger.info(String.format("Found CA cert %s", getCaCert().getSubject().toString()));

            logger.info("Loading server key and certificate");
            loadServerKeyAndCert();
            logger.info(String.format("Found server cert %s", getServerCert().getSubject().toString()));

            logger.info("Loading CRL");
            updateCrlFromDb();
        }
        catch (Exception ex) {
            logger.severe(String.format("Cannot init Pki: %s", ex.toString()));
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.severe(sw.toString());
        }
    }

    @Override
    @PreDestroy
    public void destroy() {
        super.destroy();
    }

    public String getDhFilename() {
        return folderFactory.getPkiDir() + FN_DH;
    }

    public String getCrlFilename() {
        return folderFactory.getPkiDir() + FN_CRL;
    }

    public void createTables()
                throws IOException, SQLException, ClassNotFoundException
    {
        logger.info("Creating tables for propertiesStorage...");
        String resourceName = "/WEB-INF/sql/create-pki-tables.sql";
        Reader r = null;
        try {
            ExternalContext ectx = FacesContext.getCurrentInstance().getExternalContext();
            InputStream is = ectx.getResourceAsStream(resourceName);
            r = new InputStreamReader(is);

            if (r == null) {
                logger.severe(String.format("Cannot open %s as resource", resourceName));
            }
            Connection con = databaseSettings.getDatabaseConnection();
            if (con == null) {
                logger.severe("Cannot get database connection");
            }
            DbUtils.executeSql(con, r);
        }
        finally {
            if (r != null) {
                try {
                    r.close();
                }
                catch (IOException ex) {
                    logger.severe(String.format("Cannot close reader: %s", ex.getMessage()));
                }
            }
        }
    }

    public void removeKeyAndCert(X509CertificateHolder cert)
            throws ClassNotFoundException, SQLException
    {
        if (cert == null) {
            logger.info("Cannot remove null certificate");
            return;
        }

        Connection con = databaseSettings.getDatabaseConnection();
        Statement stm = con.createStatement();
        String sql = String.format(
                "DELETE FROM certificates WHERE serial ='%s';",
                cert.getSerialNumber().toString());
        stm.executeUpdate(sql);
    }

    public void revoveCert(X509CertificateHolder cert)
        throws CRLException, ClassNotFoundException, IOException,
            OperatorCreationException, SQLException
    {
        Connection con = databaseSettings.getDatabaseConnection();
        Statement stm = con.createStatement();
        String sql = String.format(
                "UPDATE certificates SET isRevoked = true WHERE serial = '%s';",
                cert.getSerialNumber().toString()
        );
        logger.info(String.format("Executing %s", sql));
        int noRecords = stm.executeUpdate(sql);
        logger.info(String.format("%d records updated", noRecords));

        addCertificateToCrl(cert);
        writeCrl();
    }

    private void writeCrl()
            throws CRLException, IOException
    {
        PrintWriter pr = new PrintWriter(getCrlFilename());
        writeCrl(pr);
        pr.close();
    }

    private void addKeyAndCert(CertType type, PrivateKey key, X509CertificateHolder cert)
            throws ClassNotFoundException, IOException, SQLException
    {
        if (cert == null || key == null) {
            logger.severe("Certificate and key must not be null");
            return;
        }

        Connection con = databaseSettings.getDatabaseConnection();
        String sql = "INSERT INTO certificates " +
                "(serial, commonName, validFrom, validTo, certificate, privateKey, certtype)" +
                "VALUES (?, ?, ?, ?, ?, ?, ?::certtype);";
        PreparedStatement stm = con.prepareStatement(sql);
        int pos = 1;
        stm.setString(pos++, cert.getSerialNumber().toString(10));

        String cnStr = getCertCn(cert);

        stm.setString(pos++, cnStr);
        stm.setDate(pos++, new java.sql.Date(cert.getNotBefore().getTime()));
        stm.setDate(pos++, new java.sql.Date(cert.getNotAfter().getTime()));
        stm.setBytes(pos++, cert.getEncoded());
        stm.setBytes(pos++, key.getEncoded());
        stm.setString(pos++, type.toString());
        if (stm.executeUpdate() != 1) {
            logger.warning(String.format("Cannot add certificate %s", cert.getSubject().toString()));
        }
        else {
            logger.info(String.format("Successfully added certificate %s", cert.getSubject().toString()));
        }
    }

    private KeyAndCert createUserKeyAndCert(String username)
            throws NoSuchAlgorithmException, OperatorCreationException
    {
        X509CertificateHolder cert;

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
        if (!clientCertificateSettings.getOrganizationalUnit().isEmpty())
            sw.append(", OU=" + clientCertificateSettings.getOrganizationalUnit());
        if (!clientCertificateSettings.getOrganization().isEmpty())
            sw.append(", O=" + clientCertificateSettings.getOrganization());
        if (!clientCertificateSettings.getCity().isEmpty())
            sw.append(", L=" + clientCertificateSettings.getCity());
        if (!clientCertificateSettings.getState().isEmpty())
            sw.append(", ST=" + clientCertificateSettings.getState());
        if (!clientCertificateSettings.getCountry().isEmpty())
            sw.append(", C=" + clientCertificateSettings.getCountry());

        X500Name subject = new X500Name(sw.toString());
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
                clientCertificateSettings.getSignatureAlgorithm()));
        cert = createCertificate(certKey.getPublic(),
                new Time(fromDate), new Time(toDate),
                subject,
                clientCertificateSettings.getSignatureAlgorithm());

        if (cert == null) {
            logger.severe("Something went wrong. certificate is null");
        }

        return new KeyAndCert(certKey.getPrivate(), cert);
    }

    public KeyAndCert getUserKeyAndCert(String username)
        throws ClassNotFoundException, GeneralSecurityException,
            IOException, SQLException, OperatorCreationException
    {
        KeyAndCert kac = getKeyAndCert(CertType.CLIENT, username);

        if (kac == null) {
            kac = createUserKeyAndCert(username);
            addKeyAndCert(CertType.CLIENT, kac.getKey(), kac.getCert());
        }

        return kac;
    }

    private KeyAndCert getKeyAndCert(CertType type, String cn)
            throws ClassNotFoundException, PSQLException, SQLException, GeneralSecurityException, IOException
    {
        KeyAndCert ret = null;
        X509CertificateHolder cert = null;
        PrivateKey key = null;

        Connection con = databaseSettings.getDatabaseConnection();
        Statement stm = con.createStatement();
        String sql;
        if (cn == null)
            sql = String.format(
                    "SELECT commonName, certificate, privateKey FROM certificates WHERE certtype = '%s' AND isRevoked = false;",
                    type.toString());
        else
            sql = String.format(
                    "SELECT commonName, certificate, privatekey FROM certificates WHERE certtype = '%s' AND isRevoked = false AND commonName = '%s';",
                    type.toString(), cn);
        logger.info(sql);
        ResultSet result = stm.executeQuery(sql);
        if (result.next()) {
            logger.info(String.format("Found %s", result.getString("commonName")));
            byte[] bytes;

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            bytes = result.getBytes("certificate");
            cert = new X509CertificateHolder(bytes);

            KeyFactory kf = KeyFactory.getInstance("RSA");
            bytes = result.getBytes("privateKey");
            key = kf.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        }

        if (key != null & cert != null) {
            ret = new KeyAndCert(key, cert);
        }
        else {
            if (key == null) {
                logger.warning("Cannot create private key");
            }
            if (cert == null) {
                logger.warning("Cannot create certificate");
            }
        }

        return ret;
    }

    private void loadCaKeyAndCert()
            throws ClassNotFoundException, PSQLException, SQLException, CertificateException, IOException, GeneralSecurityException
    {
        KeyAndCert kac = getKeyAndCert(Pki.CertType.CA, null);
        if (kac != null) {
            setCaCert(kac.getCert());
            setCaKey(kac.getKey());
        }
    }

    private void loadServerKeyAndCert()
            throws ClassNotFoundException, SQLException, CertificateException, IOException, GeneralSecurityException
    {
        KeyAndCert kac = getKeyAndCert(Pki.CertType.SERVER, null);
        if (kac != null) {
            serverCert = kac.getCert();
            serverKey = kac.getKey();
        }
    }

    public X509CertificateHolder getServerCert() {
        try {
            if (serverCert == null)
                loadCaKeyAndCert();
        }
        catch (ClassNotFoundException | GeneralSecurityException | IOException | SQLException ex) {
            logger.severe(String.format("Cannot load server certificate and key:",
                    ex.getMessage()));
        }

        return serverCert;
    }

    public PrivateKey getServerKey() {
        try {
            if (serverKey == null)
                loadCaKeyAndCert();
        }
        catch (ClassNotFoundException | GeneralSecurityException | IOException | SQLException ex) {
            logger.severe(String.format("Cannot load server certificate and key:",
                    ex.getMessage()));
        }

        return serverKey;
    }

    public void updateCrlFromDb()
        throws CertificateException, ClassNotFoundException, IOException,
            OperatorCreationException, SQLException
    {
        logger.info("Loading CRL from database");
        Date now = new Date();
        Date nextUpdate = new Date(now.getTime() + (long) 1000 * 60 * 60 * 24 * 31);
        BcX509ExtensionUtils extUtils = new BcX509ExtensionUtils();
        X500Name crlName = new X500Name("cn=CRL");

        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(crlName, now);
        crlBuilder.setNextUpdate(nextUpdate);

        AuthorityKeyIdentifier aki = extUtils.createAuthorityKeyIdentifier(getCaCert());
        crlBuilder.addExtension(Extension.authorityKeyIdentifier, false, aki);

        Connection con = databaseSettings.getDatabaseConnection();
        Statement stm = con.createStatement();
        String sql = "SELECT certificate FROM certificates WHERE isRevoked = true;";
        ResultSet result = stm.executeQuery(sql);
        while (result.next()) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            byte[] bytes = result.getBytes("certificate");
            X509CertificateHolder cert = new X509CertificateHolder(bytes);

            logger.info(String.format("Adding certificate with serial %s...", cert.getSerialNumber().toString()));
            crlBuilder.addCRLEntry(cert.getSerialNumber(), now, CRLReason.superseded);
        }

        AlgorithmNameFinder algoFinder = new DefaultAlgorithmNameFinder();
        String signAlgoName = algoFinder.getAlgorithmName(getCaCert().getSignatureAlgorithm());

        ContentSigner sigGen = new JcaContentSignerBuilder(signAlgoName)
                    .setProvider("BC")
                    .build(getCaKey());
        setCrl(crlBuilder.build(sigGen));
    }

    public void loadCrl()
            throws CRLException, IOException, CertificateException,
            NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, SignatureException,
            ClassNotFoundException, SQLException, OperatorCreationException
    {
        FileInputStream fis;
        String fn = folderFactory.getPkiDir() + FN_CRL;
        logger.info(String.format("Loading CRL from %s...", fn));

        try {
            fis = new FileInputStream(fn);
            // setCrl(readCrl(fis));
            updateCrlFromDb();
        }
        catch (FileNotFoundException ex) {
            logger.info(String.format("CRL %s does not exist. Creating new file", fn));
            createCrl();
            updateCrlFromDb();
            logger.info("Writing CRL");
            try (PrintWriter pw = new PrintWriter(fn)) {
                writeCrl(pw);
            }
        }
    }

    public void setServerKeyAndCert(PrivateKey key, X509CertificateHolder cert)
            throws ClassNotFoundException, IOException, SQLException
    {
        logger.info(String.format("Set server certificate: %s",
                cert.getSubject().toString()));

        this.serverKey = key;
        this.serverCert = cert;

        addKeyAndCert(CertType.SERVER, key, cert);
    }

    public void saveCaKeyAndCert()
            throws ClassNotFoundException, IOException, SQLException
    {
        logger.info(String.format("Saving CA cert: %s", getCaCert().getSubject().toString()));

        addKeyAndCert(CertType.CA, getCaKey(), getCaCert());
    }

    public List<X509CertificateHolder> getAllUserCerts()
            throws CertificateException, ClassNotFoundException,
            IOException, SQLException
    {
        List<X509CertificateHolder> certs = new LinkedList<>();

        Connection con = databaseSettings.getDatabaseConnection();
        Statement stm = con.createStatement();
        String sql = "SELECT certificate FROM certificates WHERE certtype = 'CLIENT';";
        ResultSet result = stm.executeQuery(sql);

        while (result.next()) {
            byte[] bytes = result.getBytes("certificate");
            X509CertificateHolder cert = new X509CertificateHolder(bytes);
            if (cert == null) {
                logger.warning("Cannot load certificate");
            }
            else {
                certs.add(cert);
            }
        }

        return certs;
    }

}

