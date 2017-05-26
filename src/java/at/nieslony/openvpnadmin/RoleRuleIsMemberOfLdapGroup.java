/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.LdapSettings;
import at.nieslony.openvpnadmin.beans.RoleRuleIsLdapUserFactory;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapGroup;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;

/**
 *
 * @author claas
 */
public class RoleRuleIsMemberOfLdapGroup
        extends RoleRule
        implements Serializable
{
    transient private LdapSettings ldapSettings = null;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private LdapSettings getLdapSettings() {
        if (ldapSettings == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            ldapSettings = (LdapSettings) context.getExternalContext().getApplicationMap().get("ldapSettings");
            if (ldapSettings == null)
                logger.severe("Cannot find attribute ldapSettings");
        }

        return ldapSettings;
    }

    public RoleRuleIsMemberOfLdapGroup() {
    }

    public void init(RoleRuleIsLdapUserFactory factory, String groupName) {
        super.init(factory, groupName);
    }

    @Override
    public boolean isAssumedByUser(AbstractUser user) {
        if (!(user instanceof LdapUser)) {
            logger.info(String.format("User %s is not a LDAP user => not member of LDAP group",
                    user.getUsername()));
            return false;
        }

        try {
            LdapGroup group = getLdapSettings().findLdapGroup(getValue());
            return group.hasMember((LdapUser) user);
        }
        catch (NamingException | NoSuchLdapGroup ex) {
            logger.severe(ex.getMessage());
            return false;
        }
    }
}
