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

package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.openvpnadmin.RoleRuleIsUser;
import at.nieslony.utils.classfinder.StaticMemberBean;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 *
 * @author claas
 */
public class RoleRuleIsUserFactory
        implements RoleRuleFactory, Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @StaticMemberBean
    static private LdapSettings ldapSettings = null;

    static public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    private LdapSettings getLdapSettings() {
        return ldapSettings;
    }

    /**
     * Creates a new instance of RoleRuleIsUserFactory
     */
    public RoleRuleIsUserFactory() {
    }

    @Override
    public RoleRule createRule(String username) {
        RoleRuleIsUser rule = new RoleRuleIsUser();
        rule.init(this, username);

        return rule;
    }

    @Override
    public String getRoleRuleName() {
        return "isUser";
    }

    @Override
    public String getDescriptionString() {
        return "Username equals";
    }

    @Override
    public List<String> completeValue(String userPattern) {
        List<String> users = new LinkedList<>();

        DirContext ctx;
        NamingEnumeration results;
        StringBuilder pattern = new StringBuilder();

        pattern.append("(|(");
        pattern.append(getLdapSettings().getAttrUsername()).append("=").append(userPattern).append("*");
        pattern.append(")(");
        pattern.append(getLdapSettings().getAttrFullName()).append("=*").append(userPattern).append("*");
        pattern.append("))");
        logger.info(String.format("Performing LDAP search %s", pattern.toString()));

        try {
            ctx = getLdapSettings().getLdapContext();
            SearchControls sc = new SearchControls();
            sc.setCountLimit(10);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search(getLdapSettings().getOuUsers(), pattern.toString(), sc);
            while (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                Attributes attrs = result.getAttributes();
                if (attrs == null)
                    continue;
                Attribute attr = attrs.get(getLdapSettings().getAttrUsername());
                if (attr == null)
                    continue;
                String uid = (String) attr.get();
                String fullName = (String) attrs.get(getLdapSettings().getAttrFullName()).get();
                StringBuilder user = new StringBuilder();
                user.append(uid);
                if (fullName != null && !fullName.isEmpty()) {
                    user.append(" (").append(fullName).append(")");
                }
                users.add(user.toString());
            }
        }
        catch (NamingException ex) {
            logger.severe(String.format("Error while user lookup: %s", ex.getMessage()));
        }
        return users;
    }

    @Override
    public boolean getNeedsValue() {
        return true;
    }

    @Override
    public String getValueLabel() {
        return "User name";
    }
}
