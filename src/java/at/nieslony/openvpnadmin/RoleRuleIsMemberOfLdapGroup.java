/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.RoleRuleIsLdapUserFactory;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapGroup;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.naming.NamingException;

/**
 *
 * @author claas
 */
public class RoleRuleIsMemberOfLdapGroup
        extends RoleRule
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());
    RoleRuleIsLdapUserFactory factory;

    public RoleRuleIsMemberOfLdapGroup() {
    }

    public void init(RoleRuleIsLdapUserFactory factory, String groupName) {
        super.init(factory, groupName);
        this.factory = factory;
    }

    @Override
    public boolean isAssumedByUser(AbstractUser user) {
        if (!(user instanceof LdapUser)) {
            logger.info(String.format("User %s is not a LDAP user => not member of LDAP group",
                    user.getUsername()));
            return false;
        }

        try {
            LdapGroup group = RoleRuleIsLdapUserFactory.getLdapSettings().findLdapGroup(getValue());
            return group.hasMember((LdapUser) user);
        }
        catch (NamingException | NoSuchLdapGroup ex) {
            logger.severe(ex.getMessage());
            return false;
        }
    }
}
