/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.openvpnadmin.RoleRuleIsUser;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
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
@ManagedBean(eager=true)
@ApplicationScoped
public class RoleRuleIsUserFactory
        implements RoleRuleFactory, Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());
    private LdapSettings ldapSettings = null;

    @ManagedProperty(value = "#{roleRuleFactoryCollection}")
    private RoleRuleFactoryCollection roleRuleFactoryCollection;

    public void setRoleRuleFactoryCollection(RoleRuleFactoryCollection rrfc) {
        roleRuleFactoryCollection = rrfc;
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


    /**
     * Creates a new instance of RoleRuleIsUserFactory
     */
    public RoleRuleIsUserFactory() {
    }

    @PostConstruct
    public void init() {
        roleRuleFactoryCollection.addRoleRuleFactory(this);
    }

    @Override
    public RoleRule createRule(String username) {
        RoleRuleIsUser rule = new RoleRuleIsUser(this, username);

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
}
