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
