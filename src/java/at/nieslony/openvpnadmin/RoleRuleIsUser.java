/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.LdapSettings;
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
public class RoleRuleIsUser implements RoleRule, Serializable {
    private String username;
    private static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    public RoleRuleIsUser() {
    }

    public RoleRuleIsUser(String username) {
        this.username = username;
    }

    @Override
    public void setValue(String username) {
        this.username = username;
    }

    @Override
    public String getValue() {
        return username;
    }

    @Override
    public boolean isAssumedByUser(String user) {
        return this.username.equals(user);
    }

    @Override
    public String getRoleType() {
        return "Username";
    }

    @Override
    public List<String> completeValue(String userPattern, LdapSettings ldapSettings) {
        List<String> users = new LinkedList<>();

        DirContext ctx;
        NamingEnumeration results;
        StringBuilder pattern = new StringBuilder();

        pattern.append("(|(");
        pattern.append(ldapSettings.getAttrUsername()).append("=").append(userPattern).append("*");
        pattern.append(")(");
        pattern.append(ldapSettings.getAttrFullName()).append("=*").append(userPattern).append("*");
        pattern.append("))");
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
                Attribute attr = attrs.get(ldapSettings.getAttrUsername());
                if (attr == null)
                    continue;
                String uid = (String) attr.get();
                String fullName = (String) attrs.get(ldapSettings.getAttrFullName()).get();
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
}
