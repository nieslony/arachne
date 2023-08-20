
package at.nieslony.openvpnadmin.beans;

import at.nieslony.databasepropertiesstorage.PropertyGroup;
import at.nieslony.openvpnadmin.LdapGroup;
import at.nieslony.openvpnadmin.LdapHelper;
import at.nieslony.openvpnadmin.LdapHelperUser;
import at.nieslony.openvpnadmin.LdapUser;
import at.nieslony.openvpnadmin.beans.base.LdapSettingsBase;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapGroup;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapUser;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

@ManagedBean
@ApplicationScoped
public class LdapSettings
    extends LdapSettingsBase
    implements Serializable, LdapHelperUser
{
    public LdapSettings() {
        ldapHelper = new LdapHelper(this);
    }

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{propertiesStorage}")
    private PropertiesStorageBean propertiesStorage;

    private LdapHelper ldapHelper;

    public void setPropertiesStorage(PropertiesStorageBean ps) {
        propertiesStorage = ps;
    }

    protected PropertyGroup getPropertyGroup() {
        PropertyGroup  pg = null;

        try {
            return propertiesStorage.getGroup("ldap-settings", true);
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot get property group ldap-settings: %s",
                ex.getMessage()));
            if (ex.getNextException() != null)
            logger.severe(String.format("Cannot get property group ldap-settings: %s",
                ex.getNextException().getMessage()));
        }

        return null;
    }

    public LdapUser findVpnUser(String username)
            throws NamingException, NoSuchLdapUser
    {
        return ldapHelper.findVpnUser(username);
    }

    public LdapGroup findLdapGroup(String groupname)
            throws NamingException, NoSuchLdapGroup
    {
        return ldapHelper.findLdapGroup(groupname);
    }

    @Override
    public String getGroupSearchFilter(String group) {
        return ldapHelper.getGroupSearchString(group);
    }

    public DirContext getLdapContext()
            throws NamingException
    {
        return ldapHelper.getLdapContext();
    }

    public List<LdapUser> findVpnUsers(String pattern) {
        return ldapHelper.findVpnUsers(pattern);
    }

    public boolean auth(String dn, String password) {
        return ldapHelper.auth(dn, password);
    }
}
