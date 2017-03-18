/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.ClientCertificateSettings;
import at.nieslony.utils.pki.CertificateAuthority;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

/**
 *
 * @author claas
 */
@ManagedBean
@SessionScoped
public class EditClientCertificateSettings implements Serializable {
    private List<SelectItem> signatureAlgorithms;

    /**
     * Creates a new instance of ClientCertificateSettings
     */
    public EditClientCertificateSettings() {
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

        onLoad();
    }

    @ManagedProperty(value = "#{clientCertificateSettings}")
    private ClientCertificateSettings clientCertificateSettings;

    public void setClientCertificateSettings(ClientCertificateSettings ccs) {
        clientCertificateSettings = ccs;
    }

    private String signatureAlgorithm;
    private int keySize = 2048;
    private String title;
    private String organization;
    private String organizationalUnit;
    private String city;
    private String state;
    private String country;
    private int validTime = 365;
    private String validTimeUnit = "days";

    public void onLoad() {
        signatureAlgorithm = clientCertificateSettings.getSignatureAlgorith();
        keySize = clientCertificateSettings.getKeySize();
        title = clientCertificateSettings.getTitle();
        organization = clientCertificateSettings.getOrganization();
        organizationalUnit = clientCertificateSettings.getOPrganizationalUnit();
        city = clientCertificateSettings.getCity();
        state = clientCertificateSettings.getState();
        country = clientCertificateSettings.getCountry();
        validTime = clientCertificateSettings.getValidTime();
        validTimeUnit = clientCertificateSettings.getValidTimeUnit();
    }

    public void onSave() {
        clientCertificateSettings.setSignatureAlgorith(signatureAlgorithm);
        clientCertificateSettings.setKeySize(keySize);
        clientCertificateSettings.setTitle(title);
        clientCertificateSettings.setOrganization(organization);
        clientCertificateSettings.setOrganizationalUnit(organizationalUnit);
        clientCertificateSettings.setCity(city);
        clientCertificateSettings.setState(state);
        clientCertificateSettings.setCountry(country);
        clientCertificateSettings.setValidTime(validTime);
        clientCertificateSettings.setValidTimeUnit(validTimeUnit);

        clientCertificateSettings.save();
    }

    public String getValidTimeUnit() {
        return validTimeUnit;
    }

    public void setValidTimeUnit(String validTimeUnit) {
        this.validTimeUnit = validTimeUnit;
    }

    public String[] getValidTimeUnits() {
        return new String[] {
            "days",
            "months",
            "years"
        };
    }

    public int getValidTime() {
        return validTime;
    }

    public void setValidTime(int vt) {
        validTime = vt;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getOrganizationalUnit() {
        return organizationalUnit;
    }

    public void setOrganizationalUnit(String organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public List<SelectItem> getSignatureAlgorithms() {
        return signatureAlgorithms;
    }

    public String getKeyAlgo() {
        return signatureAlgorithm.split("with")[1];
    }

    public int[] getKeySizes() {
        for (CertificateAuthority.KeySignAlgo ksa : CertificateAuthority.getKeySignAlgos()) {
            if (ksa.keyAlgo.equals(getKeyAlgo()))
                return ksa.keySizes;
        }

        return new int[0];
    }
}
