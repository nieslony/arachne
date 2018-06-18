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

import java.io.Serializable;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

/**
 *
 * @author claas
 */
public class LdapUser
        extends AbstractUser
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private LdapHelperUser ldapHelperUser;
    private String dn;

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getDn() {
        return dn;
    }

    public LdapUser(LdapHelperUser ls, String username) {
        ldapHelperUser = ls;
        super.setUsername(username);
    }

    public void setLdapAttributes(Attributes attrs)
            throws NamingException
    {
        Attribute attr;
        attr = attrs.get(ldapHelperUser.getAttrFullName());
        if (attr != null)
            setFullName((String) attr.get());

        attr = attrs.get(ldapHelperUser.getAttrGivenName());
        if (attr != null)
            setGivenName((String) attr.get());

        attr = attrs.get(ldapHelperUser.getAttrSurname());
        if (attr != null)
            setSurName((String) attr.get());

        attr = attrs.get(ldapHelperUser.getAttrEmail());
        if (attr !=  null)
            setEmail((String) attr.get());
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    public boolean auth(String password) {
        return ldapHelperUser.auth(getDn(), password);
    }

    @Override
    public String getUserTypeStr() {
        return "LDAP";
    }
}
