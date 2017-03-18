
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.beans.base.ClientCertificateSettingsBase;
import at.nieslony.openvpnadmin.beans.PropertiesStorageBean;
import java.io.Serializable;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import java.sql.SQLException;
import java.util.logging.Logger;
import at.nieslony.databasepropertiesstorage.PropertyGroup;

@ManagedBean
@ApplicationScoped
public class ClientCertificateSettings
    extends ClientCertificateSettingsBase
    implements Serializable
{
    public ClientCertificateSettings() {
    }

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{propertiesStorage}")
    private PropertiesStorageBean propertiesStorage;

    public void setPropertiesStorage(PropertiesStorageBean ps) {
        propertiesStorage = ps;
    }

    protected PropertyGroup getPropertyGroup() {
        PropertyGroup  pg = null;

        try {
            return propertiesStorage.getGroup("client-certificate-settings", true);
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot get property group client-certificate-settings: %s",
                ex.getMessage()));
            if (ex.getNextException() != null)
            logger.severe(String.format("Cannot get property group client-certificate-settings: %s",
                ex.getNextException().getMessage()));
        }

        return null;
    }

    public String getKeyAlgorithm() {
        String sa = getSignatureAlgorithm();
        String[] saSplit = sa.split("with");
        if (saSplit.length != 2) {
            return "???";
        }
        else {
            return saSplit[1];
        }
    }

}
