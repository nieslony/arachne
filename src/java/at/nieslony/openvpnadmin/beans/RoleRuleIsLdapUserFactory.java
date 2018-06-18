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
import at.nieslony.openvpnadmin.RoleRuleIsMemberOfLdapGroup;
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
public class RoleRuleIsLdapUserFactory
        implements RoleRuleFactory, Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @StaticMemberBean
    static private LdapSettings ldapSettings = null;

    static public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    static public LdapSettings getLdapSettings() {
        return ldapSettings;
    }
    /**
     * Creates a new instance of RoleRuleFactoryIsLdapUser
     */
    public RoleRuleIsLdapUserFactory() {
    }

    @Override
    public RoleRule createRule(String groupname) {
        RoleRuleIsMemberOfLdapGroup rule = new RoleRuleIsMemberOfLdapGroup();
        rule.init(this, groupname);

        return rule;
    }

    @Override
    public String getRoleRuleName() {
        return "isMemberOfLdapGroup";
    }

    @Override
    public String getDescriptionString() {
        return "Is member of LDAP group";
    }


    @Override
    public List<String> completeValue(String userPattern) {
        List<String> groups = new LinkedList<>();

        DirContext ctx;
        NamingEnumeration results;
        StringBuilder pattern = new StringBuilder();
        pattern.append("(&(");
            pattern.append("objectClass=").append(getLdapSettings().getObjectClassGroup());
        pattern.append(")(");
            pattern.append("|(");
            pattern.append(getLdapSettings().getAttrGroupName()).append("=").append(userPattern).append("*");
            pattern.append(")(");
            pattern.append(getLdapSettings().getAttrGroupDescription()).append("=*").append(userPattern).append("*");
            pattern.append("))");
        pattern.append(")");
        logger.info(String.format("Performing LDAP search %s", pattern.toString()));

        try {
            ctx = getLdapSettings().getLdapContext();
            SearchControls sc = new SearchControls();
            sc.setCountLimit(10);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search(getLdapSettings().getOuGroups(), pattern.toString(), sc);
            if (!results.hasMore())
                logger.info("Nothing found");
            while (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                Attributes attrs = result.getAttributes();
                if (attrs == null) {
                    logger.info(String.format("Group %s has no attributes", result.getNameInNamespace()));
                    continue;
                }
                Attribute attr = attrs.get(getLdapSettings().getAttrGroupName());
                if (attr == null) {
                    logger.info(String.format("Group %s has no name", result.getNameInNamespace()));
                    continue;
                }
                String gid = (String) attr.get();
                String description = (String) attr.get();
                StringBuilder group = new StringBuilder();
                group.append(gid);
                if (description != null && !description.isEmpty()) {
                    group.append(" (").append(description).append(")");
                }
                logger.info(String.format("Found group %s", group.toString()));
                groups.add(group.toString());
            }
        }
        catch (NamingException ex) {
            logger.severe(String.format("An error occured during LDAP search: %s",
                    ex.getMessage()));
        }
        return groups;
    }
}
