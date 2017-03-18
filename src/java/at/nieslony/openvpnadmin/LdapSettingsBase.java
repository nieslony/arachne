/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.exceptions.NoSuchLdapGroup;
import at.nieslony.utils.NetUtils;
import java.net.InetAddress;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
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
public class LdapSettingsBase {
    private Properties props = new Properties();
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    protected final static String PROP_PROVIDER_URL = "provider-url";
    protected final static String PROP_ATTR_USERNAME = "attr-username";
    protected final static String PROP_ATTR_FULL_NAME = "attr-full-name";
    protected final static String PROP_ATTR_GIVEN_NAME = "attr-given-name";
    protected final static String PROP_ATTR_SURNAME = "attr-surname";
    protected final static String PROP_AUTH_TYPE = "auth-type";
    protected final static String PROP_OBJECTCLASS_USER = "objectclass-user";
    protected final static String PROP_OU_USERS = "ou-users";
    protected final static String PROP_OBJECTCLASS_GROUP = "objectclass-group";
    protected final static String PROP_ATTR_GROUPNAME = "attr-groupname";
    protected final static String PROP_ATTR_GROUP_DESCRIPTION = "attr-group-description";
    protected final static String PROP_OU_GROUPS = "ou-groups";
    protected final static String PROP_ATTR_GROUP_MEMBER_UID = "attr-group-member";
    protected final static String PROP_ATTR_GROUP_MEMBER_DN = "attr-group-member-dn";
    protected final static String PROP_SECURITY_PRINCIPAL = "security-principal";
    protected final static String PROP_SECURITY_CREDENTIALS = "security-credentials";
    protected final static String PROP_MEMBER_ATTR_TYPE = "member-attr-type";

    public enum MemberAttrType {
        MAT_MEMBER_DN,
        MAT_MEMBER_UID
   }

    public MemberAttrType getMemberAttrType() {
        MemberAttrType ret = MemberAttrType.MAT_MEMBER_DN;

        try {
            ret = MemberAttrType.valueOf(getProps().getProperty(
                    PROP_MEMBER_ATTR_TYPE,
                    ret.toString()));
        }
        catch (IllegalArgumentException ex) {
            logger.severe(ex.getMessage());
        }

        return ret;
    }

    public void setMemberAttrType(MemberAttrType memberAttrType) {
        if (memberAttrType != null)
            getProps().setProperty(PROP_MEMBER_ATTR_TYPE, memberAttrType.name());
        else
            logger.warning("memberAttrType==null");
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    public Properties getProps() {
        return props;
    }

    public String getSecurityPrincipal() {
        return getProps().getProperty(PROP_SECURITY_PRINCIPAL, "");
    }

    public void setSecurityPrincipal(String sp) {
        getProps().setProperty(PROP_SECURITY_PRINCIPAL, sp);
    }

    public String getSecurityCredentials() {
        String base64 = getProps().getProperty(PROP_SECURITY_CREDENTIALS, "");
        if (!base64.isEmpty())
            return new String(Base64.getDecoder().decode(base64));
        else
            return base64;
    }

    public void setSecurityCredentials(String sc) {
        if (sc != null && !sc.isEmpty()) {
            String base64 = new String(
                    Base64.getEncoder().encode(sc.getBytes())
            );
            getProps().setProperty(PROP_SECURITY_CREDENTIALS, base64);
        }
    }

    public String getAttrGroupMemberUid() {
        return getProps().getProperty(PROP_ATTR_GROUP_MEMBER_UID, "memberUid");
    }

    public void setAttrGroupMemberUid(String attr) {
        getProps().setProperty(PROP_ATTR_GROUP_MEMBER_UID, attr);
    }

    public String getAttrGroupMemberDn() {
        return getProps().getProperty(PROP_ATTR_GROUP_MEMBER_DN, "member");
    }

    public void setAttrGroupMemberDn(String attr) {
        getProps().setProperty(PROP_ATTR_GROUP_MEMBER_DN, attr);
    }

    public String getDefaultProviderUrl() {
        String myDomain = NetUtils.myDomain();
        String defaultHost = null;
        logger.info(String.format("DNS lookupup"
                + " for my domain %s", myDomain));
        try {
            logger.info("DNS lookup LDAP srv record");
            defaultHost = NetUtils.srvLookup("ldap");
            logger.info(String.format("Found: %s", defaultHost));
        }
        catch (NamingException ex) {
            logger.info(String.format("No entry founnd: %s", ex.getMessage()));
        }
        if (defaultHost == null) {
            defaultHost = "ldap." + myDomain;
            try {
                logger.info(String.format("DNS lookup %s", defaultHost));
                InetAddress addr = InetAddress.getByName(defaultHost);
                logger.info(String.format("Found: %s", addr.getHostAddress()));
            }
            catch (Exception e) {
                logger.info(String.format("No entry foind: %s", e.getMessage()));
                defaultHost = "ldap.example.com";
            }
        }

        String myDomSplit[] = myDomain.split("\\.");
        for (int i = 0; i < myDomSplit.length; i++) {
            myDomSplit[i] = "dc=" + myDomSplit[i];
        }
        String defaultDn = String.join(",", myDomSplit);

        return String.format("ldap://%s/%s", defaultHost, defaultDn);
    }

    public String getProviderUrl() {
        return getProps().getProperty(PROP_PROVIDER_URL, getDefaultProviderUrl());
    }

    public void setProviderUrl(String url) {
        getProps().setProperty(PROP_PROVIDER_URL, url);
    }

    public String getAttrUsername() {
        return getProps().getProperty(PROP_ATTR_USERNAME, "uid");
    }

    public void setAttrUsername(String un) {
        getProps().setProperty(PROP_ATTR_USERNAME, un);
    }

    public String getAttrFullName() {
        return getProps().getProperty(PROP_ATTR_FULL_NAME, "cn");
    }

    public void setAttrFullName(String fn) {
        getProps().setProperty(PROP_ATTR_FULL_NAME, fn);
    }

    public String getAttrGivenName() {
        return getProps().getProperty(PROP_ATTR_GIVEN_NAME, "givenName");
    }

    public void setAttrGivenName(String gn) {
        getProps().setProperty(PROP_ATTR_GIVEN_NAME, gn);
    }

    public String getAttrSurname() {
        return getProps().getProperty(PROP_ATTR_SURNAME, "sn");
    }

    public void setAttrSurname(String sn) {
        getProps().setProperty(PROP_ATTR_SURNAME, sn);
    }

    public String getAuthType() {
        return getProps().getProperty(PROP_AUTH_TYPE, "none");
    }

    public void setAuthType(String at) {
        getProps().setProperty(PROP_AUTH_TYPE, at);
    }

    public void setObjectClassUser(String oc) {
        getProps().setProperty(PROP_OBJECTCLASS_USER, oc);
    }

    public String getObjectClassUser() {
        return getProps().getProperty(PROP_OBJECTCLASS_USER, "posixAccount");
    }

    public String getUserSearchString(String username) {
        return String.format("(&(objectClass=%s)(%s=%s))",
                getObjectClassUser(), getAttrUsername(),  username);
    }

    public String getGroupSearchString(String groupname) {
        return String.format("(&(objectClass=%s)(%s=%s))",
                getObjectClassGroup(), getAttrGroupname(),  groupname);
    }

    public String getOuUsers() {
        return getProps().getProperty(PROP_OU_USERS, "");
    }

    public void setOuUsers(String ou) {
        getProps().setProperty(PROP_OU_USERS, ou);
    }

    public String getObjectClassGroup() {
        return getProps().getProperty(PROP_OBJECTCLASS_GROUP, "groupOfNames");
    }

    public void setObjectClassGroup(String oc) {
        getProps().setProperty(PROP_OBJECTCLASS_GROUP, oc);
    }

    public String getOuGroups() {
        return getProps().getProperty(PROP_OU_GROUPS, "");
    }

    public void setOuGroups(String oug) {
        getProps().setProperty(PROP_OU_GROUPS, oug);
    }

    public String getAttrGroupname() {
        return getProps().getProperty(PROP_ATTR_GROUPNAME, "cn");
    }

    public void setAttrGroupname(String agn) {
        getProps().setProperty(PROP_ATTR_GROUPNAME, agn);
    }

    public String getAttrGroupDescription() {
        return getProps().getProperty(PROP_ATTR_GROUP_DESCRIPTION, "description");
    }

    public void setAttrGroupDescription(String agd) {
        getProps().setProperty(PROP_ATTR_GROUP_DESCRIPTION, agd);
    }

    public String getBaseDn() {
        String url = getProviderUrl();
        try {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            return "";
        }
    }

    public DirContext getLdapContext() throws NamingException {
        Hashtable<String,String> env = new Hashtable<>();
        env.put(Context.SECURITY_AUTHENTICATION, getAuthType());
        logger.info(String.format("LDAP bind to %s", getProviderUrl()));
        if (getAuthType().equals("simple")) {
            logger.info(String.format("bind type simple => etting principal %s and password", getSecurityPrincipal()));
            env.put(Context.SECURITY_PRINCIPAL, //"cn=ldap-ro,cn=groups,cn=compat,dc=nieslony,dc=lan"
                    getSecurityPrincipal()
            );
            env.put(Context.SECURITY_CREDENTIALS, //"Moin123"
                    getSecurityCredentials()
            );
        }
        else {
            logger.info("LDAP bind without authentication");
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, getProviderUrl());

        return new InitialDirContext(env);
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
        results = ctx.search(getOuGroups(), getGroupSearchString(groupname), sc);
        if (results.hasMore()) {
            group = new LdapGroup();

            SearchResult result = (SearchResult) results.next();
            logger.info(String.format("Creating group from %s", result.getName()));
            Attributes attrs = result.getAttributes();
            Attribute attr;
            attr = attrs.get(getAttrGroupname());
            if (attr != null)
                group.setName((String) attr.get());
            attr = attrs.get(getAttrGroupDescription());
            if (attr != null)
                group.setDescription((String) attr.get());
            switch (getMemberAttrType()) {
                case MAT_MEMBER_DN:
                    logger.info(String.format("memberAttrType=%s, processing attribute %s",
                            getMemberAttrType().name(), getAttrGroupMemberDn()));
                    attr = attrs.get(getAttrGroupMemberDn());
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
                            getMemberAttrType().name(), getAttrGroupMemberUid()));
                    attr = attrs.get(getAttrGroupMemberUid());
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

}
