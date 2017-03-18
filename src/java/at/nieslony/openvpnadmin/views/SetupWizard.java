/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.RoleRuleIsUser;
import at.nieslony.openvpnadmin.VpnUser;
import at.nieslony.openvpnadmin.beans.LocalUsers;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.Roles;
import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import at.nieslony.utils.pki.CertificateAuthority;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.security.auth.x500.X500Principal;
import org.primefaces.event.FlowEvent;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class SetupWizard implements Serializable {
    public class KeyAlgoData implements  Serializable{
    }

    private static final long serialVersionUID = 467254444471451527L;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private String adminUserName = "admin";
    private String password;

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

    private boolean performingSetup = false;

    private List<SelectItem> signatureAlgorithms;

    @ManagedProperty(value = "#{pki}")
    private Pki pki;

    @ManagedProperty(value = "#{localUsers}")
    private LocalUsers localUsers;

    @ManagedProperty(value = "#{roles}")
    private Roles rolesBean;

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

    private void saveCA() throws GeneralSecurityException, IOException {
        performingSetup = true;

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
        X500Principal issuerDN = new X500Principal(sw.toString());
        X500Principal subjectDN = new X500Principal(sw.toString());
        String keyAlgo = getCaKeyAlgo();
        pki.createSelfSignedCa(caStartDate, caEndDate, issuerDN, subjectDN,
                caSignatureAlgorithm,
                keyAlgo, caKeySize);
        FileWriter fw = new FileWriter(pki.getCaDir() + "/ca.crt");
        pki.writeCaCert(new PrintWriter(fw));
        fw.close();
        fw = new FileWriter(pki.getCaDir() + "/ca.key");
        pki.writeCaKey(new PrintWriter(fw));
        fw.close();

        performingSetup = false;
    }

    private void saveServerCert() throws GeneralSecurityException, IOException {
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
        X500Principal subjectDN = new X500Principal(sw.toString());
        pki.setServerCert(pki.createCertificate(certKey.getPublic(),
                certStartDate, certEndDate,
                subjectDN, certSignatureAlgorithm));
        pki.setServerKey(certKey.getPrivate());
        FileWriter fw = new FileWriter(pki.getCaDir() + "/server.crt");
        pki.writeCertificate(pki.getServerCert(), new PrintWriter(fw));
        fw.close();

        fw = new FileWriter(pki.getCaDir() + "/server.key");
        pki.writePrivateKey(certKey.getPrivate(), new PrintWriter(fw));
        fw.close();

        fw = new FileWriter(pki.getDhFilename());
        pki.writeDhParameters(new PrintWriter(fw));
        fw.close();
    }

    public void onSave() throws GeneralSecurityException, IOException {
        VpnUser admin = localUsers.addUser(adminUserName, password);
        admin.setFullName("Master Administrator");
        rolesBean.addRule("admin", new RoleRuleIsUser(adminUserName));
        rolesBean.save();
        saveCA();
        saveServerCert();

        FacesContext.getCurrentInstance().getExternalContext().redirect("Login.xhtml");
    }

    public void setLocalUsers(LocalUsers localUsers) {
        this.localUsers = localUsers;
    }

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    public void setRolesBean(Roles rolesBean) {
        this.rolesBean = rolesBean;
    }

    public void requireNoSetup(ComponentSystemEvent event) throws PermissionDenied {
        FacesContext fc = FacesContext.getCurrentInstance();
        if ((localUsers.isValid() || pki.isValid()) & !performingSetup) {
            logger.severe("Setup wizard already done. To re-run read documentation.");
            throw new PermissionDenied();
        }
    }

    public List<SelectItem> getSignatureAlgorithms() {
        return signatureAlgorithms;
    }
}
