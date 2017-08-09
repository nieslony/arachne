
package at.nieslony.openvpnadmin.beans;

import at.nieslony.databasepropertiesstorage.PropertyGroup;
import at.nieslony.openvpnadmin.beans.base.AuthSettingsBase;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

@ManagedBean
@ApplicationScoped
public class AuthSettings
    extends AuthSettingsBase
    implements Serializable
{
    public AuthSettings() {
    }

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{propertiesStorage}")
    private PropertiesStorageBean propertiesStorage;

    public void setPropertiesStorage(PropertiesStorageBean ps) {
        propertiesStorage = ps;
    }

    @Override
    protected PropertyGroup getPropertyGroup() {
        PropertyGroup  pg = null;

        try {
            return propertiesStorage.getGroup("auth-settings", true);
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot get property group auth-settings: %s",
                ex.getMessage()));
            if (ex.getNextException() != null)
            logger.severe(String.format("Cannot get property group auth-settings: %s",
                ex.getNextException().getMessage()));
        }

        return null;
    }
}
