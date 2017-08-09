/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.beans.DatabaseSettings;
import at.nieslony.openvpnadmin.beans.FolderFactory;
import at.nieslony.openvpnadmin.beans.LocalUserFactory;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.PropertiesStorageBean;
import at.nieslony.openvpnadmin.beans.Roles;
import at.nieslony.openvpnadmin.beans.TaskScheduler;
import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import at.nieslony.utils.pki.CertificateAuthority;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.encoders.Hex;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class SetupWizard implements Serializable {
    private static final long serialVersionUID = 467254444471451527L;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    public enum CaType {
        SELF_SIGNED,
        REMOTE_SIGNED
    }

    private String adminUserName = "admin";
    private String password;

    private CaType caType = CaType.REMOTE_SIGNED;

    private String caTitle = "OpenVPN_Admin_CA";
    private String caCommonName = "OpenVPN Admin self signed CA";
    private String caOrganization = new String();
    private String caOrganizationalUnit = new String();
    private String caCity = new String();
    private String caState = new String();
    private String caCountry = new String();
    private String[] caEmail;
    private Date caStartDate = new Date();
    private Date caEndDate = new Date(caStartDate.getTime() + 1000L * 60L * 60L * 24L * 3650L);
    private String caSignatureAlgorithm = "SHA512withRSA";
    private int caKeySize = 2048;

    private String csrTitle = "OpenVPN_Admin_CA";
    private String csrCommonName = "OpenVPN Admin self signed CA";
    private String csrOrganization = new String();
    private String csrOrganizationalUnit = new String();
    private String csrCity = new String();
    private String csrState = new String();
    private String csrCountry = new String();
    private String[] csrEmail;
    private Date csrStartDate = new Date();
    private Date csrEndDate = new Date(csrStartDate.getTime() + 1000L * 60L * 60L * 24L * 3650L);
    private String csrSignatureAlgorithm = "SHA512withRSA";
    private int csrKeySize = 2048;
    private String csrText = new String();
    private String csrSignedCsr = new String();

    private String certTitle = new String();
    private String certCommonName = new String();
    private String certOrganization = new String();
    private String certOrganizationalUnit = new String();
    private String certCity = new String();
    private String certState = new String();
    private String certCountry = new String();
    private String[] certEmail;
    private Date certStartDate = new Date();
    private Date certEndDate = new Date(caStartDate.getTime() + 1000L * 60L * 60L * 24L * 364L);
    private String certSignatureAlgorithm = "SHA512withRSA";
    private int certKeySize = 2048;

    private String databaseHost = "localhost";
    private int  databasePort = 5432;
    private String databaseUser = "openvpnadmin";
    private String databasePassword = "";
    private String databaseName = "openvpnadmin";
    private String dbAdminUser = "";
    private String dbAdminPassword = "";
    private int userExistingDb = 0;

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;

    public FolderFactory getFolderFactory() {
        return folderFactory;
    }

    public void setFolderFactory(FolderFactory ff) {
        folderFactory = ff;
    }

    public int getUserExistingDb() {
        return userExistingDb;
    }

    public void setUserExistingDb(int userExistingDb) {
        this.userExistingDb = userExistingDb;
    }

    public String getDbAdminPassword() {
        return dbAdminPassword;
    }

    public void setDbAdminPassword(String daAdminPassword) {
        this.dbAdminPassword = daAdminPassword;
    }

    public String getDbAdminUser() {
        return dbAdminUser;
    }

    public void setDbAdminUser(String dbAdminUser) {
        this.dbAdminUser = dbAdminUser;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public void setDatabaseUser(String databseUser) {
        this.databaseUser = databseUser;
    }

    public int getDatabasePort() {
        return databasePort;
    }

    public void setDatabasePort(int databasePort) {
        this.databasePort = databasePort;
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public void setDatabaseHost(String databaseHost) {
        this.databaseHost = databaseHost;
    }

    private boolean performingSetup = false;

    private List<SelectItem> signatureAlgorithms;

    @ManagedProperty(value = "#{pki}")
    private Pki pki;

    @ManagedProperty(value = "#{roles}")
    private Roles rolesBean;

    @ManagedProperty(value = "#{databaseSettings}")
    private DatabaseSettings databaseSettings;

    @ManagedProperty(value = "#{propertiesStorage}")
    private PropertiesStorageBean propertiesStorage;

    @ManagedProperty(value = "#{localUserFactory}")
    private LocalUserFactory localUserFactory;

    @ManagedProperty(value = "#{roles}")
    private Roles roles;

    @ManagedProperty(value = "#{taskScheduler}")
    private TaskScheduler taskScheduler;

    /**
     * Creates a new instance of SetupWizardBean
     */
    public SetupWizard(){
        try {
            certTitle = InetAddress.getLocalHost().getHostName();
            certCommonName = certTitle;
        }
        catch (UnknownHostException ex) {
            certTitle = "unknown hostname";
        }
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
                items[i] = new SelectItem(value, label);
            }
            group.setSelectItems(items);
            signatureAlgorithms.add(group);
        }
    }

    public String getCaKeyAlgo() {
        return caSignatureAlgorithm.split("with")[1];
    }

    public int[] getCaKeySizes() {
        for (CertificateAuthority.KeySignAlgo ksa : CertificateAuthority.getKeySignAlgos()) {
            if (ksa.keyAlgo.equals(getCaKeyAlgo()))
                return ksa.keySizes;
        }

        return new int[0];
    }

    public int[] getCsrKeySizes() {
        for (CertificateAuthority.KeySignAlgo ksa : CertificateAuthority.getKeySignAlgos()) {
            if (ksa.keyAlgo.equals(getCaKeyAlgo()))
                return ksa.keySizes;
        }

        return new int[0];
    }

    public String getCertKeyAlgo() {
        return certSignatureAlgorithm.split("with")[1];
    }

    public int[] getCertKeySizes() {
        for (CertificateAuthority.KeySignAlgo ksa : CertificateAuthority.getKeySignAlgos()) {
            if (ksa.keyAlgo.equals(getCertKeyAlgo()))
                return ksa.keySizes;
        }

        return new int[0];
    }

    public String getCaSignatureAlgorithm() {
        return caSignatureAlgorithm;
    }

    public void setCaSignatureAlgorithm(String a) {
        caSignatureAlgorithm = a;
    }

    public int getCaKeySize() {
        return caKeySize;
    }

    public void setCaKeySize(int s) {
        caKeySize = s;
    }

    public String getCsrSignatureAlgorithm() {
        return csrSignatureAlgorithm;
    }

    public void setCsrSignatureAlgorithm(String a) {
        csrSignatureAlgorithm = a;
    }

    public int getCsrKeySize() {
        return csrKeySize;
    }

    public void setCsrKeySize(int s) {
        csrKeySize = s;
    }

    public int getCertKeySize() {
        return certKeySize;
    }

    public void setCertKeySize(int s) {
        certKeySize = s;
    }

    public String getCertSignatureAlgorithm() {
        return certSignatureAlgorithm;
    }

    public void setCertSignatureAlgorithm(String a) {
        certSignatureAlgorithm = a;
    }

    public String getAdminUserName() {
        return adminUserName;
    }

    public void setAdminUserName(String adminUserName) {
        this.adminUserName = adminUserName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCaTitle(String caTitle) {
        this.caTitle = caTitle;
    }

    public String getCaTitle() {
        return caTitle;
    }

    public void setCaCommonName(String caCommonName) {
        this.caCommonName = caCommonName;
    }

    public String getCaCommonName() {
        return caCommonName;
    }

    public void setCaOrganization(String caOrganization) {
        this.caOrganization = caOrganization;
    }

    public String getCaOrganization() {
        return caOrganization;
    }

    public void setCaOrganizationalUnit(String caOrganizationalUnit) {
        this.caOrganizationalUnit = caOrganizationalUnit;
    }

    public String getCaOrganizationalUnit() {
        return caOrganizationalUnit;
    }

    public void setCaCity(String caCity) {
        this.caCity = caCity;
    }

    public String getCaCity() {
        return caCity;
    }

    public void setCaState(String caState) {
        this.caState = caState;
    }

    public String getCaState() {
        return caState;
    }

    public void setCaCountry(String caCountry) {
        this.caCountry = caCountry;
    }

    public String getCaCountry() {
        return caCountry;
    }

    public void setCaStartDate(Date date) {
        this.caStartDate = date;
    }

    public Date getCaStartDate() {
        return caStartDate;
    }

    public void setCaEndDate(Date date) {
        this.caEndDate = date;
    }

    public Date getCaEndDate() {
        return caEndDate;
    }

    public void setCsrText(String t) {
        csrText = t;
    }

    public String getCsrText() {
        return csrText;
    }

    public void setCsrSignedCsr(String s) {
        csrSignedCsr = s;
    }

    public String getCsrSignedCsr() {
        return csrSignedCsr;
    }

    public void setCsrTitle(String csrTitle) {
        this.csrTitle = csrTitle;
    }

    public String getCsrTitle() {
        return csrTitle;
    }

    public StreamedContent getCsrAsFile() {
        String csr = "nix";
        InputStream is = new ByteArrayInputStream(csr.getBytes());

        StreamedContent sc = new DefaultStreamedContent(is, "text/plain", "arachne-ca.scr");

        return sc;
    }

    public void setCsrCommonName(String csrCommonName) {
        this.csrCommonName = csrCommonName;
    }

    public String getCsrCommonName() {
        return csrCommonName;
    }

    public void setCsrOrganization(String csrOrganization) {
        this.csrOrganization = csrOrganization;
    }

    public String getCsrOrganization() {
        return csrOrganization;
    }

    public void setCsrOrganizationalUnit(String csrOrganizationalUnit) {
        this.csrOrganizationalUnit = csrOrganizationalUnit;
    }

    public String getCsrOrganizationalUnit() {
        return csrOrganizationalUnit;
    }

    public void setCsrCity(String csrCity) {
        this.csrCity = csrCity;
    }

    public String getCsrCity() {
        return csrCity;
    }

    public void setCsrState(String csrState) {
        this.csrState = csrState;
    }

    public String getCsrState() {
        return csrState;
    }

    public void setCsrCountry(String csrCountry) {
        this.csrCountry = csrCountry;
    }

    public String getCsrCountry() {
        return csrCountry;
    }

    public void setCsrStartDate(Date date) {
        this.csrStartDate = date;
    }

    public Date getCsrStartDate() {
        return csrStartDate;
    }

    public void setCsrEndDate(Date date) {
        this.csrEndDate = date;
    }

    public Date getCsrEndDate() {
        return csrEndDate;
    }

    public void setCertTitle(String certTitle) {
        this.certTitle = certTitle;
    }

    public String getCertTitle() {
        return certTitle;
    }

    public void setCertCommonName(String certCommonName) {
        this.certCommonName = certCommonName;
    }

    public String getCertCommonName() {
        return certCommonName;
    }

    public void setCertOrganization(String certOrganization) {
        this.certOrganization = certOrganization;
    }

    public String getCertOrganization() {
        return certOrganization;
    }

    public void setCertOrganizationalUnit(String certOrganizationalUnit) {
        this.certOrganizationalUnit = certOrganizationalUnit;
    }

    public String getCertOrganizationalUnit() {
        return certOrganizationalUnit;
    }

    public void setCertCity(String certCity) {
        this.certCity = certCity;
    }

    public String getCertCity() {
        return certCity;
    }

    public void setCertState(String certState) {
        this.certState = certState;
    }

    public String getCertState() {
        return certState;
    }

    public void setCertCountry(String certCountry) {
        this.certCountry = certCountry;
    }

    public String getCertCountry() {
        return certCountry;
    }

     public void setCertStartDate(Date date) {
        this.certStartDate = date;
    }

    public Date getCertStartDate() {
        return certStartDate;
    }

    public void setCertEndDate(Date date) {
        this.certEndDate = date;
    }

    public Date getCertEndDate() {
        return certEndDate;
    }

   public void onSetupCaChanged() {

    }

    public String onFlowProcess(FlowEvent event) {
        return event.getNewStep();
    }

    private void saveCA()
            throws GeneralSecurityException, IOException, ClassNotFoundException, SQLException
    {


        StringWriter sw = new StringWriter();
        sw.append("CN=" + caCommonName);
        if (!caOrganizationalUnit.isEmpty())
            sw.append(", OU=" + caOrganizationalUnit);
        if (!caOrganization.isEmpty())
            sw.append(", O=" + caOrganization);
        if (!caCity.isEmpty())
            sw.append(", L=" + caCity);
        if (!caState.isEmpty())
            sw.append(", ST=" + caState);
        if (!caCountry.isEmpty())
            sw.append(", C=" + caCountry);
        X500Name issuerDN = new X500Name(sw.toString());
        X500Name subjectDN = new X500Name(sw.toString());

        String keyAlgo = getCaKeyAlgo();
        pki.createSelfSignedCa(new Time(caStartDate), new Time(caEndDate), issuerDN, subjectDN,
                caSignatureAlgorithm,
                keyAlgo, caKeySize);
        pki.saveCaKeyAndCert();
    }

    private void saveServerCert()
        throws ClassNotFoundException, IOException, NoSuchAlgorithmException,
            SQLException, OperatorCreationException
    {
        StringWriter sw = new StringWriter();
        sw.append("CN=" + certCommonName);
        if (!certOrganizationalUnit.isEmpty())
            sw.append(", OU=" + certOrganizationalUnit);
        if (!certOrganization.isEmpty())
            sw.append(", O=" + certOrganization);
        if (!certCity.isEmpty())
            sw.append(", L=" + certCity);
        if (!certState.isEmpty())
            sw.append(", ST=" + certState);
        if (!certCountry.isEmpty())
            sw.append(", C=" + certCountry);
        KeyPairGenerator keygen = KeyPairGenerator.getInstance(getCertKeyAlgo());
        keygen.initialize(certKeySize, new SecureRandom());
        KeyPair certKey = keygen.generateKeyPair();
        X500Name subjectDN = new X500Name(sw.toString());

        X509CertificateHolder cert = pki.createCertificate(certKey.getPublic(),
                new Time(caStartDate), new Time(caEndDate),
                subjectDN, caSignatureAlgorithm);
        pki.setServerKeyAndCert(certKey.getPrivate(), cert);
    }

    private void saveDhParams()
            throws IOException
    {
        FileWriter fw;

        fw = new FileWriter(pki.getDhFilename());
        pki.writeDhParameters(new PrintWriter(fw));
        fw.close();
    }

    void saveDatabaseSettings() {
        databaseSettings.setDatabaseName(databaseName);
        databaseSettings.setDatabaseUser(databaseUser);
        databaseSettings.setDatabasePassword(databasePassword);
        databaseSettings.setHost(databaseHost);
        databaseSettings.setPort(databasePort);

        try {
            databaseSettings.save();
            databaseSettings.closeDatabaseConnection();
        }
        catch (IOException | SQLException ex) {
            logger.severe(String.format("Cannot save databse settings: %s", ex.toString()));
        }
    }

    public void setupTaskScheduler()
            throws ClassNotFoundException, IOException, SQLException
    {
        taskScheduler.createTables();
        taskScheduler.init();
    }

    public void onSave() {
        performingSetup = true;
        String step = "";

        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        logger.info("--- Begin application setup ---");
        try {
            step = "Saving database settings";
            logger.info(step);
            saveDatabaseSettings();

            step = "Initializiong properties storage";
            logger.info(step);
            propertiesStorage.createTables();

            step = "Initializing local users and roles";
            logger.info(step);
            localUserFactory.createTables();

            step = "Creating admin user";
            logger.info(step);
            AbstractUser adminUser = localUserFactory.addUser(adminUserName);
            adminUser.setFullName("Master Administrator");
            adminUser.setPassword(password);
            adminUser.save();

            step = String.format("Assigning role admin to user %s", adminUserName);
            logger.info(step);
            roles.load();
            roles.addRule("admin", "isUser", "admin");

            step = "Creating CA";
            pki.createTables();

            saveCA();

            step = "Creating server certificate";
            logger.info(step);
            saveServerCert();

            step = "Creating DH parameters";
            logger.info(step);
            saveDhParams();

            pki.init();

            step = "Scheduling tasks";
            setupTaskScheduler();

            performingSetup = false;

            logger.info("Setup successful, redirecting to login page");
            ec.redirect("Login.xhtml");
        }
        catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.printf("Caught %s exception duriung application setup.\n",
                    ex.getClass().getName());
            pw.println(ex.getMessage());
            ex.printStackTrace(pw);

            fc.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error",
                            String.format("%s: %s", step, ex.getMessage())
                            )
                    );

            logger.severe(sw.toString());
        }
        finally {
            logger.info("--- End application setup ---");
        }
    }

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    public void setRolesBean(Roles rolesBean) {
        this.rolesBean = rolesBean;
    }

    public void requireNoSetup(ComponentSystemEvent event) throws PermissionDenied {
        if (performingSetup) {
            logger.info("Still performing setup. Access to setup wizard granted");
            return;
        }

        FacesContext fc = FacesContext.getCurrentInstance();
        if (databaseSettings.isValid()) {
            logger.severe("Setup wizard already done. To re-run read documentation.");
            throw new PermissionDenied();
        }
    }

    public List<SelectItem> getSignatureAlgorithms() {
        return signatureAlgorithms;
    }

    public String getDatabaseUrl() {
        String url = String.format("jdbc:postgresql://%s:%d/%s",
                databaseHost, databasePort, databaseName);

        return url;
    }

    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }

    public void setLocalUserFactory(LocalUserFactory luf) {
        localUserFactory = luf;
    }

    public void setPropertiesStorage(PropertiesStorageBean ps) {
        propertiesStorage = ps;
    }

    public void onTestDatabaseConnection() {
        Connection con = null;
        String message = "Database connection seems to work";
        try {
            Class.forName("org.postgresql.Driver");
            String conUrl = String.format("jdbc:postgresql://%s:%d/%s",
                    databaseHost,
                    databasePort,
                    databaseName);
            con = DriverManager.getConnection(conUrl, databaseUser, databasePassword);
            if (con == null)
                message = "Cannot create database connection";
        }
        catch (ClassNotFoundException | SQLException ex) {
            message = ex.getMessage();
        }
        finally {
            if (con != null) {
                try {
                    con.close();
                }
                catch (SQLException ex) {
                    message = String.format("Cannot close database connection: %s",
                            ex.getMessage());
                }
            }
        }


        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Test database connection",
                message
        );

        RequestContext.getCurrentInstance().showMessageInDialog(facesMessage);
    }

    public void setRoles(Roles roles) {
        this.roles = roles;
    }

    public void setTaskScheduler(TaskScheduler ts) {
        taskScheduler = ts;
    }

    public CaType getCaType() {
        return caType;
    }

    public void setCaType(CaType ct) {
        caType = ct;
    }

    public String getSqlCreateDatabase() {
        FileInputStream fis = null;
        BufferedReader br;
        String sql = "";

        String filename = folderFactory.getSqlDir() + "/setup-database.sql";
        String pwdHash;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException ex) {
            logger.severe(String.format("Cannot get MD5: %s", ex.getMessage()));
            return sql;
        }
        byte[] md5Sum = digest.digest(String.format("%s%s", databasePassword, databaseUser).getBytes());
        pwdHash = String.format("md5%s", Hex.toHexString(md5Sum));

        try {
            fis = new FileInputStream(filename);
            br = new BufferedReader(new InputStreamReader(fis));

            String line;
            StringBuilder buf = new StringBuilder();
            buf.append("-- cat <<EOF | sudo -u postgres psql\n");
            while( (line = br.readLine()) != null) {
                buf.append(line.replaceAll("DB_NAME", databaseName)
                        .replaceAll("DB_USER", databaseUser)
                        .replaceAll("DB_PASSWORD", pwdHash));
                buf.append("\n");
            }
            buf.append("-- EOF\n");
            sql = buf.toString();
        }
        catch (FileNotFoundException ex) {
            String msg = String.format("Cannot open %s: %s",
                    filename, ex.getMessage());
            logger.warning(msg);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
        }
        catch (IOException ex) {
            String msg = String.format("Error reading %s: %s",
                    filename, ex.getMessage());
            logger.warning(msg);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
        }
        finally {
            if (fis != null) {
                try {
                    fis.close();
                }
                catch (IOException ex) {
                    logger.warning(String.format("Cannot close %s: %s",
                            filename, ex.getMessage()));
                }
            }
        }

        return sql;
    }

    public StreamedContent getSqlCreateDatabaseFile() {
        String sql = getSqlCreateDatabase();

        InputStream is = new ByteArrayInputStream(sql.getBytes());
        StreamedContent sc = new DefaultStreamedContent(is, "text/sql", "create-arachne-database.sql");

        return sc;
    }
}
