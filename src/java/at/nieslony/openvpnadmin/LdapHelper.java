/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.base.LdapSettingsBase;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapGroup;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapUser;
import at.nieslony.utils.NetUtils;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 *
 * @author claas
 */
public class LdapHelper
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    LdapHelperUser ldapHelperUser;

    public LdapHelper(LdapHelperUser lhu) {
        ldapHelperUser = lhu;
    }

    public List<LdapUser> findVpnUsers(String pattern) {
        DirContext ctx;
        NamingEnumeration results;
        LinkedList<LdapUser> users = new LinkedList<>();

        try {
            ctx = getLdapContext();

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search(ldapHelperUser.getOuUsers(), getUserSearchString(pattern), sc);
            while (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                Attributes attrs = result.getAttributes();
                Attribute attr;
                LdapUser vpnUser;

                attr = attrs.get(ldapHelperUser.getAttrUsername());
                if (attr != null) {
                    vpnUser = new LdapUser(ldapHelperUser, (String) attr.get());
                }
                else {
                    logger.warning("Ignoring user with no username");
                    continue;
                }

                vpnUser.setLdapAttributes(attrs);
                String dn;
                if (ldapHelperUser.getOuUsers() != null &
                        !ldapHelperUser.getOuUsers().isEmpty()
                        )
                    dn = String.format("%s,%s,%s",
                            result.getName(),
                            ldapHelperUser.getOuUsers(),
                            getBaseDn());
                else
                    dn = String.format("%s,%s",
                            result.getName(),
                            getBaseDn());
                vpnUser.setDn(dn);

                users.add(vpnUser);
            }
        }
        catch (NamingException ex) {
            logger.severe(String.format("Error finding VPN users in LDAP: %s",
                    ex.getMessage()));
        }

        return users;
    }

    public LdapUser findVpnUser(String username)
            throws NoSuchLdapUser, NamingException
    {
        String searchString = getUserSearchString(username);
        logger.info(String.format("Trying to find user %s in LDAP. Search: %s",
                username, searchString));

        DirContext ctx;
        NamingEnumeration results;
        LdapUser vpnUser = null;

        ctx = getLdapContext();

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        results = ctx.search(ldapHelperUser.getOuUsers(), searchString, sc);
        if (results.hasMore()) {
            vpnUser = new LdapUser(ldapHelperUser, username);
            SearchResult result = (SearchResult) results.next();
            Attributes attrs = result.getAttributes();

            vpnUser.setLdapAttributes(attrs);
            String dn;
            if (ldapHelperUser.getOuUsers() != null &
                    !ldapHelperUser.getOuUsers().isEmpty()
                    )
                dn = String.format("%s,%s,%s",
                        result.getName(),
                        ldapHelperUser.getOuUsers(),
                        getBaseDn());
            else
                dn = String.format("%s,%s",
                        result.getName(),
                        getBaseDn());
            vpnUser.setDn(dn);
        }
        else {
            throw new NoSuchLdapUser(String.format("LDAP user %s not found", username));
        }

        return vpnUser;
    }

    public String getDefaultUserSearchString(String username) {
        return String.format("(&(objectClass=%s)(%s=%s))",
                ldapHelperUser.getObjectClassUser(), ldapHelperUser.getAttrUsername(),  username);
    }

    public String getUserSearchString(String username) {
        if (ldapHelperUser.getUseCustomUserSearchFilter()) {
            return ldapHelperUser.getCustomUserSearchFilter().replaceAll("%u", username);
        }

        return getDefaultUserSearchString(username);
    }

    public LdapGroup findLdapGroup(String groupname)
            throws NoSuchLdapGroup, NamingException
    {
        logger.info(String.format("Trying to find group %s in LDAP", groupname));

        DirContext ctx;
        NamingEnumeration results;
        LdapGroup group = null;

        ctx = getLdapContext();

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        results = ctx.search(ldapHelperUser.getOuGroups(), getGroupSearchString(groupname), sc);
        if (results.hasMore()) {
            group = new LdapGroup();

            SearchResult result = (SearchResult) results.next();
            logger.info(String.format("Creating group from %s", result.getName()));
            Attributes attrs = result.getAttributes();
            Attribute attr;
            attr = attrs.get(ldapHelperUser.getAttrGroupName());
            if (attr != null)
                group.setName((String) attr.get());
            attr = attrs.get(ldapHelperUser.getAttrGroupDescription());
            if (attr != null)
                group.setDescription((String) attr.get());
            switch (ldapHelperUser.getMemberAttrType()) {
                case MAT_MEMBER_DN:
                    logger.info(String.format("memberAttrType=%s, processing attribute %s",
                            ldapHelperUser.getMemberAttrType().name(), ldapHelperUser.getAttrGroupMemberDn()));
                    attr = attrs.get(ldapHelperUser.getAttrGroupMemberDn());
                    if (attr != null) {
                        NamingEnumeration values;
                        values = attr.getAll();
                        List<String> dns = group.getMemberDNs();
                        dns.clear();
                        while (values.hasMore()) {
                            String member = (String) values.next();
                            logger.info(String.format("Found member %s", member));
                            dns.add(member);
                        }
                    }
                    else {
                        logger.info("No members found");
                    }
                    break;
                case MAT_MEMBER_UID:
                    logger.info(String.format("memberAttrType=%s, processing attribute %s",
                            ldapHelperUser.getMemberAttrType().name(), ldapHelperUser.getAttrGroupMemberUid()));
                    attr = attrs.get(ldapHelperUser.getAttrGroupMemberUid());
                    if (attr != null) {
                        NamingEnumeration values;
                        values = attr.getAll();
                        List<String> uids = group.getMemberUids();
                        uids.clear();
                        while (values.hasMore()) {
                            String member = (String) values.next();
                            logger.info(String.format("Found member %s", member));
                            uids.add(member);
                        }
                    }
                    else {
                        logger.info("No members found");
                    }
                    break;
            }
        }
        else {
            throw new NoSuchLdapGroup(String.format("LDAP group %s not found", groupname));
        }

        return group;
    }

    public String getDefaultGroupSearchString(String groupname) {
        return String.format("(&(objectClass=%s)(%s=%s))",
                ldapHelperUser.getObjectClassGroup(), ldapHelperUser.getAttrGroupName(),  groupname);
    }

    public String getGroupSearchString(String groupname) {
        if (ldapHelperUser.getUseCustomGroupSearchFilter()) {
            return ldapHelperUser.getCustomGroupSearchFilter().replaceAll("%g", groupname);
        }

        return getDefaultGroupSearchString(groupname);
    }

    public DirContext getLdapContext() throws NamingException {
        Hashtable<String,String> env = new Hashtable<>();
        env.put(Context.SECURITY_AUTHENTICATION, ldapHelperUser.getAuthType());

        String url = formLdapUrl();
        logger.info(String.format("LDAP bind to %s", url));
        if (ldapHelperUser.getAuthType().equals("simple")) {
            logger.info(String.format("bind type simple => getting principal %s and password", ldapHelperUser.getSecurityPrincipal()));
            env.put(Context.SECURITY_PRINCIPAL, //"cn=ldap-ro,cn=groups,cn=compat,dc=nieslony,dc=lan"
                    ldapHelperUser.getSecurityPrincipal()
            );
            env.put(Context.SECURITY_CREDENTIALS, //"Moin123"
                    ldapHelperUser.getSecurityCredentials()
            );
        }
        else {
            logger.info("LDAP bind without authentication");
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);

        return new InitialDirContext(env);
    }

    public String getBaseDn() {
        String url = ldapHelperUser.getProviderUrl();
        try {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            return "";
        }
    }

    public boolean auth(String dn, String password)
    {
        Hashtable<String,String> env = new Hashtable<>();

        boolean ok = true;

        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapHelperUser.getProviderUrl());

        try {
            DirContext ctx = new InitialDirContext(env);
        }
        catch (NamingException ex) {
            logger.warning(String.format("Cannot authticate DN %s: %s",
                    dn, ex.getMessage()));

            ok = false;
        }

        return ok;
    }

    public String formLdapUrl() {
        String servername;
        if (ldapHelperUser.getLdapServereLookupMethod() == LdapSettingsBase.LdapServerLookupMethod.HOSTNAME) {
            servername = ldapHelperUser.getLdapServer();
        }
        else {
            String dnsDomain = ldapHelperUser.getLdapDnsDomain();
            try {
                servername = NetUtils.srvLookup("ldap", dnsDomain);
            }
            catch (NamingException | NullPointerException ex) {
                logger.warning(String.format("Cannot find LDAP SRV record for domain %s: %s",
                        dnsDomain, ex.getMessage()));
                servername = "ldap.server.not.found.in.dns";
            }
        }

        Integer port = ldapHelperUser.getLdapPort();
        String baseDn = ldapHelperUser.getLdapBaseDn();

        String url = String.format("ldap://%s:%d/%s", servername, port, baseDn);
        logger.info(String.format("LDAP url: %s", url));

        return url;
    }
}
