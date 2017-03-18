/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.LdapSettings;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapGroup;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapUser;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
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
public class RoleRuleIsMemberOfLdapGroup implements RoleRule, Serializable {
    private String groupName = null;
    private LdapSettings ldapSettings = null;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    public RoleRuleIsMemberOfLdapGroup() {
    }

    private LdapSettings getLdapSettings() {
        if (ldapSettings == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            ldapSettings = (LdapSettings) context.getExternalContext().getApplicationMap().get("ldapSettings");
            if (ldapSettings == null)
                logger.severe("Cannot find attribute ldapSettings");
        }

        return ldapSettings;
    }

    public RoleRuleIsMemberOfLdapGroup(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public void setValue(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String getValue() {
        return groupName;
    }

    @Override
    public String getRoleType() {
        return "Member of LDAP group";
    }

    @Override
    public boolean isAssumedByUser(String username) {
        LdapSettings ls = getLdapSettings();
        if (ls == null) {
            logger.severe("No LDAP settings available => no group");
            return false;
        }

        try {
            LdapGroup group = ldapSettings.findLdapGroup(groupName);
            VpnUser user = ldapSettings.findVpnUser(username);
            return group.hasMember(user);
        }
        catch (NamingException | NoSuchLdapGroup | NoSuchLdapUser ex) {
            logger.severe(ex.getMessage());
            return false;
        }
    }

    @Override
    public List<String> completeValue(String userPattern, LdapSettings ldapSettings) {
        List<String> groups = new LinkedList<>();

        DirContext ctx;
        NamingEnumeration results;
        StringBuilder pattern = new StringBuilder();
        pattern.append("(&(");
            pattern.append("objectClass=").append(ldapSettings.getObjectClassGroup());
        pattern.append(")(");
            pattern.append("|(");
            pattern.append(ldapSettings.getAttrGroupname()).append("=").append(userPattern).append("*");
            pattern.append(")(");
            pattern.append(ldapSettings.getAttrGroupDescription()).append("=*").append(userPattern).append("*");
            pattern.append("))");
        pattern.append(")");
        logger.info(String.format("Performing LDAP search %s", pattern.toString()));

        try {
            ctx = ldapSettings.getLdapContext();
            SearchControls sc = new SearchControls();
            sc.setCountLimit(10);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search(ldapSettings.getOuUsers(), pattern.toString(), sc);
            while (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                Attributes attrs = result.getAttributes();
                if (attrs == null)
                    continue;
                Attribute attr = attrs.get(ldapSettings.getAttrGroupname());
                if (attr == null)
                    continue;
                String gid = (String) attr.get();
                String description = (String) attr.get();
                StringBuilder group = new StringBuilder();
                group.append(gid);
                if (description != null && !description.isEmpty()) {
                    group.append(" (").append(description).append(")");
                }
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
