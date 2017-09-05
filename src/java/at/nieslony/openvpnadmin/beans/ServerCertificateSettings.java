
package at.nieslony.openvpnadmin.beans;

import at.nieslony.databasepropertiesstorage.PropertyGroup;
import at.nieslony.openvpnadmin.beans.base.ServerCertificateSettingsBase;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Logger;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.X509CertificateHolder;

@ManagedBean
@ApplicationScoped
public class ServerCertificateSettings
    extends ServerCertificateSettingsBase
    implements Serializable
{
    public ServerCertificateSettings() {
    }

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{propertiesStorage}")
    private PropertiesStorageBean propertiesStorage;

    public void setPropertiesStorage(PropertiesStorageBean ps) {
        propertiesStorage = ps;
    }

    @ManagedProperty(value = "#{pki}")
    private Pki pki;

    public void setPki(Pki pki) {
        this.pki = pki;
    }

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

     public void closeServerCertificateSettings(ServerCertificateEditor editor) {
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
        editor.setValidTimeUnit("days");
    }
 }
