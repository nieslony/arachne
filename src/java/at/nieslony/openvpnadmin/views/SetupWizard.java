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

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.beans.DatabaseSettings;
import at.nieslony.openvpnadmin.beans.FirewallSettings;
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
import java.io.File;
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
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Hex;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
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

    private CaType caType = CaType.SELF_SIGNED;

    private String caTitle = "Arachne_CA";
    private String caCommonName = "Arachne self signed CA";
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

    private String csrTitle = "Arachne_CA";
    private String csrCommonName = "Arachne signing CA";
    private String csrOrganization = new String();
    private String csrOrganizationalUnit = new String();
    private String csrCity = new String();
    private String csrState = new String();
    private String csrCountry = new String();
    private String[] csrEmail;
    private String csrSignatureAlgorithm = "SHA512withRSA";
    private int csrKeySize = 2048;
    private String csrText = new String();
    private String csrSignedCsr = new String();
    private String csrSigningCa = new String();

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

    private final String ICO_STEP_OPEN = "fa fa-circle-o";
    private final String ICO_STEP_WORKING = "fa fa-cog";
    private final String ICO_STEP_DONE = "fa fa-check-circle-o";

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

    @ManagedProperty(value = "#{firewallSettings}")
    private FirewallSettings firewallSettings;

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

    public String getCsrSigningCa() {
        return csrSigningCa;
    }

    public void setCsrSigningCa(String sca) {
        csrSigningCa = sca;
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
        InputStream is = new ByteArrayInputStream(csrText.getBytes());

        StreamedContent sc = new DefaultStreamedContent(is, "text/plain", "arachne-ca.scr");

        return sc;
    }

    public void handleCsrSignedCertUpload(FileUploadEvent event) {
        InputStream is;

        if (event == null) {
            logger.warning("Got null event");
            return;
        }

        if (event.getFile() == null) {
            logger.warning("Event has null file");
            return;
        }

        try {
            is = event.getFile().getInputstream();
        }
        catch (IOException ex) {
            String msg = String.format("Upload of signed CSR %s failed: %s",
                    event.getFile().getFileName(), ex.getMessage());
            logger.warning(msg);
            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            while ( (line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        catch (IOException ex) {
            String msg = String.format("Errot reading uploaded file: %s",
                    ex.getMessage());
            logger.warning(msg);
            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );
        }

        csrSignedCsr = sb.toString();
    }

    public void handleCsrSigningCaUpload(FileUploadEvent event) {
        InputStream is;

        if (event == null) {
            logger.warning("Got null event");
            return;
        }

        if (event.getFile() == null) {
            logger.warning("Event has null file");
            return;
        }

        try {
            is = event.getFile().getInputstream();
        }
        catch (IOException ex) {
            String msg = String.format("Upload of sining CA file %s failed: %s",
                    event.getFile().getFileName(), ex.getMessage());
            logger.warning(msg);
            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            while ( (line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        catch (IOException ex) {
            String msg = String.format("Errot reading uploaded file: %s",
                    ex.getMessage());
            logger.warning(msg);
            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );
        }

        csrSigningCa = sb.toString();
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

    private void createCsr()
            throws IOException, NoSuchAlgorithmException, OperatorCreationException
    {
        logger.info("Creating new CSR for CA");

        StringWriter sw = new StringWriter();
        sw.append("CN=" + csrCommonName);
        if (!csrOrganizationalUnit.isEmpty())
            sw.append(", OU=" + csrOrganizationalUnit);
        if (!csrOrganization.isEmpty())
            sw.append(", O=" + csrOrganization);
        if (!csrCity.isEmpty())
            sw.append(", L=" + csrCity);
        if (!csrState.isEmpty())
            sw.append(", ST=" + csrState);
        if (!csrCountry.isEmpty())
            sw.append(", C=" + csrCountry);

        PKCS10CertificationRequest csr = pki.createCaCsr(
                new X500Name(sw.toString()),
                caSignatureAlgorithm, getCaKeyAlgo(), caKeySize);

        StringWriter writer = new StringWriter();
        pki.writeCsr(csr, new PrintWriter(writer));

        csrText = writer.toString();
    }

    public String onFlowProcess(FlowEvent event) {
        try {
            if (event.getNewStep().equals("createCsr"))
                createCsr();
        }
        catch (IOException | NoSuchAlgorithmException | OperatorCreationException ex) {
            String msg = String.format("Error when on page switching: %s",
                    ex.getMessage());

            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", msg)
                    );

            logger.warning(msg);
        }

        return event.getNewStep();
    }

    private void saveCA()
            throws GeneralSecurityException, IOException, ClassNotFoundException, SQLException
    {
        if (caType == CaType.SELF_SIGNED) {
            StringWriter sw = new StringWriter();
            if (!caTitle.isEmpty())
                sw.append("T=" + caTitle + ",");
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
        }
        else {
            pki.setCaCert(csrSignedCsr);
        }
        pki.saveCaKeyAndCert();
    }

    private void saveServerCert()
        throws ClassNotFoundException, IOException, NoSuchAlgorithmException,
            SQLException, OperatorCreationException
    {
        StringWriter sw = new StringWriter();
        if (!certTitle.isEmpty())
            sw.append("T=" + certTitle + ",");
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

        // https://www.programcreek.com/java-api-examples/index.php?api=org.bouncycastle.asn1.x509.KeyUsage
        X509CertificateHolder cert = pki.createCertificate(certKey.getPublic(),
                new Time(certStartDate), new Time(certEndDate),
                subjectDN, certSignatureAlgorithm);
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

    public void setupFirewall()
        throws ClassNotFoundException, IOException, SQLException
    {
        firewallSettings.createTables();
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
            styleClassSaveDbSettings = ICO_STEP_WORKING;
            //rc.update("form-setup:saveDbSettingsIcon");
            saveDatabaseSettings();
            styleClassSaveDbSettings = ICO_STEP_DONE;
            //rc.update("form-setup:saveDbSettingsIcon");

            step = "Initializing properties storage";
            logger.info(step);
            styleClassInitPropertyStorage = ICO_STEP_WORKING;
            //rc.update("form-setup:initPropertyStorageIcon");
            propertiesStorage.createTables();
            styleClassInitPropertyStorage = ICO_STEP_DONE;
            //rc.update("form-setup:initPropertyStorageIcon");

            step = "Initializing local users and roles";
            logger.info(step);
            styleClassInitLocalUserAndRoles = ICO_STEP_WORKING;
            //rc.update("form-setup:initLocalUserAndRolesIcon");
            localUserFactory.createTables();
            styleClassInitLocalUserAndRoles = ICO_STEP_DONE;
            //rc.update("form-setup:initLocalUserAndRolesIcon");

            step = "Creating admin user";
            logger.info(step);
            styleClassCreateUser = ICO_STEP_WORKING;
            //rc.update("form-setup:createUserIcon");
            AbstractUser adminUser = localUserFactory.addUser(adminUserName);
            adminUser.setFullName("Master Administrator");
            adminUser.setPassword(password);
            adminUser.save();
            styleClassCreateUser = ICO_STEP_DONE;
            //rc.update("form-setup:createUserIcon");

            step = String.format("Assigning role admin to user %s", adminUserName);
            logger.info(step);
            styleClassAssignRoleAdmin = ICO_STEP_WORKING;
            //rc.update("form-setup:assignRoleAdminIcon");
            roles.load();
            roles.addRule("admin", "isUser", "admin");
            styleClassAssignRoleAdmin = ICO_STEP_DONE;
            //rc.update("form-setup:assignRoleAdminIcon");

            step = "Creating CA";
            logger.info(step);
            styleClassCreateCA = ICO_STEP_WORKING;
            //rc.update("form-setup:createCAIcon");
            pki.createTables();
            saveCA();
            styleClassCreateCA = ICO_STEP_DONE;
            //rc.update("form-setup:createCAIcon");

            step = "Creating server certificate";
            logger.info(step);
            styleClassCreateServerCertitficate = ICO_STEP_WORKING;
            //rc.update("form-setup:createServerCertitficateIcon");
            saveServerCert();
            styleClassCreateServerCertitficate = ICO_STEP_DONE;
            //rc.update("form-setup:createServerCertitficateIcon");

            step = "Creating DH parameters";
            logger.info(step);
            styleClassCreateDhParameters = ICO_STEP_WORKING;
            //rc.update("form-setup:createDhParametersIcon");
            saveDhParams();
            pki.init();
            styleClassCreateDhParameters = ICO_STEP_DONE;
            //rc.update("form-setup:createDhParametersIcon");

            step = "Scheduling tasks";
            logger.info(step);
            styleClassScheduleTasks = ICO_STEP_WORKING;
            //rc.update("form-setup:scheduleTasksIcon");
            setupTaskScheduler();
            styleClassScheduleTasks = ICO_STEP_DONE;
            //rc.update("form-setup:scheduleTasksIcon");

            step = "Firewall";
            logger.info(step);
            setupFirewall();

            performingSetup = false;

            FacesContext ctx = FacesContext.getCurrentInstance();
            ExternalContext eCtx = ctx.getExternalContext();
            String fileName = eCtx.getRealPath("/SetupWizard.xhtml");
            logger.info(String.format("Removing %s", fileName));
            File setupWizardXhtml = new File(fileName);
            setupWizardXhtml.delete();

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

    private String styleClassSaveDbSettings = "fa fa-circle-o";
    private String styleClassInitPropertyStorage = "fa fa-circle-o";
    private String styleClassInitLocalUserAndRoles = "fa fa-circle-o";
    private String styleClassCreateUser = "fa fa-circle-o";
    private String styleClassAssignRoleAdmin = "fa fa-circle-o";
    private String styleClassCreateCA = "fa fa-circle-o";
    private String styleClassCreateServerCertitficate = "fa fa-circle-o";
    private String styleClassCreateDhParameters = "fa fa-circle-o";
    private String styleClassScheduleTasks = "fa fa-circle-o";

    public String getStyleClassInitPropertyStorage() {
        return styleClassInitPropertyStorage;
    }

    public String getStyleClassInitLocalUserAndRoles() {
        return styleClassInitLocalUserAndRoles;
    }

    public String getStyleClassCreateUser() {
        return styleClassCreateUser;
    }

    public String getStyleClassAssignRoleAdmin() {
        return styleClassAssignRoleAdmin;
    }

    public String getStyleClassCreateCA() {
        return styleClassCreateCA;
    }

    public String getStyleClassCreateServerCertitficate() {
        return styleClassCreateServerCertitficate;
    }

    public String getStyleClassCreateDhParameters() {
        return styleClassCreateDhParameters;
    }

    public String getStyleClassSaveDbSettings() {
        return styleClassSaveDbSettings;
    }

    public String getStyleClassScheduleTasks() {
        return styleClassScheduleTasks;
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
        String conUrl = String.format("jdbc:postgresql://%s:%d/%s",
                databaseHost,
                databasePort,
                databaseName);
        logger.info(String.format("Testing database connection to %s", conUrl));
        try {
            Class.forName("org.postgresql.Driver");
            con = DriverManager.getConnection(conUrl, databaseUser, databasePassword);
            if (con == null)
                message = "Cannot create database connection";
        }
        catch (ClassNotFoundException | SQLException ex) {
            message = String.format("Cannot establish connection to %s: %s",
                    conUrl, ex.getMessage());
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

        logger.info(message);
        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Test database connection",
                message
        );

        PrimeFaces.current().dialog().showMessageDynamic(facesMessage);
    }

    public void setRoles(Roles roles) {
        this.roles = roles;
    }

    public void setFirewallSettings(FirewallSettings fs) {
        firewallSettings = fs;
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
