/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.TimeUnit;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.ServerCertificateEditor;
import at.nieslony.openvpnadmin.beans.ServerCertificateRenewer;
import at.nieslony.utils.pki.CaHelper;
import at.nieslony.utils.pki.CertificateAuthority;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.bouncycastle.asn1.x500.X500Name;

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

    @ManagedProperty(value = "#{pki}")
    Pki pki;

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    @ManagedProperty(value = "#{serverCertificateRenewer}")
    ServerCertificateRenewer serverCertificateRenewer;

    public void setServerCertificateRenewer(ServerCertificateRenewer scr) {
        serverCertificateRenewer = scr;
    }

    /**
     * Creates a new instance of RenewServerCertificate
     */
    public RenewServerCertificate() {
    }

    @PostConstruct
    public void init() {
        serverCertificateRenewer.setDefaultValues(this);
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
    public Integer getValidTime() {
        return validTime;
    }

    @Override
    public void setValidTimeUnit(TimeUnit unit) {
        validTimeUnit = unit;
    }

    @Override
    public TimeUnit getValidTimeUnit() {
        return validTimeUnit;
    }

    @Override
    public Integer getKeySize() {
        return keySize;
    }

    @Override
    public void setKeySize(Integer ks) {
        keySize = ks;
    }

    @Override
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    @Override
    public void setSignatureAlgorithm(String sa) {
        signatureAlgorithm = sa;
    }


    public TimeUnit[] getValidTimeUnits() {
        return TimeUnit.values();
    }

    @Override
    public X500Name getSubjectDn() {
        return CaHelper.getSubjectDN(title, commonName, organizationalUnit, organization, city, state, country);
    }

    public void onRenewServerCertificate() {
        serverCertificateRenewer.renewServerCertificate(this);
    }

    public String getKeyAlgo() {
        String[] split = getSignatureAlgorithm().split("WITH");

        if (split.length == 2)
            return split[1];
        else
            return "";
    }

    public int[] getKeySizes() {
        for (CertificateAuthority.KeySignAlgo ksa : CertificateAuthority.getKeySignAlgos()) {
            if (ksa.keyAlgo.equals(getKeyAlgo()))
                return ksa.keySizes;
        }

        return new int[0];
    }
}
