
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.beans.base.UserVpnBase;
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
public class UserVpn
    extends UserVpnBase
    implements Serializable    
{
    public UserVpn() {
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
            return propertiesStorage.getGroup("user-vpn", true);
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot get property group user-vpn: %s",
                ex.getMessage()));                
            if (ex.getNextException() != null) 
            logger.severe(String.format("Cannot get property group user-vpn: %s",
                ex.getNextException().getMessage()));                
        }
        
        return null;
    }
}
