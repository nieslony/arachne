
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.beans.base.ServerCertificateSettingsBase;
import at.nieslony.openvpnadmin.beans.PropertiesStorageBean;
import java.io.Serializable;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.logging.Logger;
import at.nieslony.databasepropertiesstorage.PropertyGroup;

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
}
