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

import at.nieslony.openvpnadmin.TimeUnit;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.ServerCertificateEditor;
import at.nieslony.openvpnadmin.beans.ServerCertificateRenewer;
import at.nieslony.utils.pki.CaHelper;
import at.nieslony.utils.pki.CertificateAuthority;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.logging.Logger;
import org.bouncycastle.asn1.x500.X500Name;

/**
 *
 * @author claas
 */
@ViewScoped
@Named
public class RenewServerCertificate
        implements ServerCertificateEditor, Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    Pki pki;

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    @Inject
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
