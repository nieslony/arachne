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

import at.nieslony.databasepropertiesstorage.PropertyGroup;
import at.nieslony.openvpnadmin.TimeUnit;
import at.nieslony.openvpnadmin.beans.base.ServerCertificateSettingsBase;
import at.nieslony.utils.pki.CaHelper;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.X509CertificateHolder;

@ApplicationScoped
@Named
public class ServerCertificateSettings
    extends ServerCertificateSettingsBase
    implements Serializable, ServerCertificateEditor
{
    public ServerCertificateSettings() {
    }

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    private PropertiesStorageBean propertiesStorage;

    public void setPropertiesStorage(PropertiesStorageBean ps) {
        propertiesStorage = ps;
    }

    @Inject
    private Pki pki;

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    @Inject
    ServerCertificateRenewer serverCertificateRenewer;

    public void setServerCertificateRenewer(ServerCertificateRenewer scr) {
        serverCertificateRenewer = scr;
    }

    @PostConstruct
    public void init() {
        if (!getValuesAlreadySet()) {
            if (serverCertificateRenewer.setDefaultValues(this))
                setValuesAlreadySet(Boolean.TRUE);
        }
    }

    @Override
    protected PropertyGroup getPropertyGroup() {
        PropertyGroup  pg = null;

        try {
            return propertiesStorage.getGroup("server-certificate-settings", true);
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot get property group server-certificate-settings: %s",
                ex.getMessage()));
            if (ex.getNextException() != null)
            logger.severe(String.format("Cannot get property group server-certificate-settings: %s",
                ex.getNextException().getMessage()));
        }

        return null;
    }

    private String getComponentFromX500Name(ASN1ObjectIdentifier id, X500Name name) {
        RDN[] cns = name.getRDNs(id);
        if (cns != null && cns.length > 0) {
            RDN cn = name.getRDNs(id)[0];
            String s = IETFUtils.valueToString(cn.getFirst().getValue());
            logger.info(String.format("{%s} %s=%s", name.toString(), id.toString(), s));
            return s;
        }
        else {
            logger.info(String.format("{%s} %s", name.toString(), id.toString()));
            return "";
        }
    }

    public void cloneServerCertificateSettings(ServerCertificateEditor editor) {
        X509CertificateHolder cert = pki.getServerCert();

        logger.info(String.format("CA: {%s} Server: {%s}",
                pki.getCaCert().getSubject().toString(),
                pki.getServerCert().getSubject().toString()));

        X500Name subject = cert.getSubject();
        editor.setTitle(getComponentFromX500Name(BCStyle.T, subject));
        editor.setCommonName(getComponentFromX500Name(BCStyle.CN, subject));
        editor.setOrganization(getComponentFromX500Name(BCStyle.O, subject));
        editor.setOrganizationalUnit(getComponentFromX500Name(BCStyle.OU, subject));
        editor.setCity(getComponentFromX500Name(BCStyle.L, subject));
        editor.setState(getComponentFromX500Name(BCStyle.ST, subject));
        editor.setCountry(getComponentFromX500Name(BCStyle.C, subject));

        Date validFrom = cert.getNotBefore();
        Date validTo = cert.getNotAfter();

        double validity = validTo.getTime() - validFrom.getTime();
        validity = validity / 1000.0 / 60.0 / 60.0 / 24.0;
        editor.setValidTime((int) Math.round(validity));
        editor.setValidTimeUnit(TimeUnit.DAY);
    }

    @Override
    public X500Name getSubjectDn() {
        return CaHelper.getSubjectDN(getTitle(), getCommonName(),
                getOrganizationalUnit(), getOrganization(),
                getCity(), getState(), getCountry());
    }

    public void renewServerCertificate() {

    }

    @Override
    public void setValidTime(Integer time) {
        super.setValidTime(time);
    }

    @Override
    public void setValidTimeUnit(TimeUnit unit) {
        super.setValidTimeUnit(unit);
    }

    @Override
    public Integer getKeySize() {
        return super.getKeySize();
    }

    @Override
    public TimeUnit getValidTimeUnit() {
        return super.getValidTimeUnit();
    }
}
