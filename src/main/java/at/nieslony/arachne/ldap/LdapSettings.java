/*
 * Copyright (C) 2023 claas
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
package at.nieslony.arachne.ldap;

import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.ArachneUser;
import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.utils.NetUtils;
import at.nieslony.arachne.utils.SrvRecord;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.kerberos.client.config.SunJaasKrb5LoginConfig;
import org.springframework.security.kerberos.client.ldap.KerberosLdapContextSource;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class LdapSettings {

    private static final Logger logger = LoggerFactory.getLogger(LdapSettings.class);

    private final static String SK_LDAP_ENABLE_USER_SOURCE = "ldap.user-source";
    private final static String SK_LDAP_URLS = "ldap.urls";
    private final static String SK_LDAP_BASE_DN = "ldap.base-dn";
    private final static String SK_LDAP_BIND_DN = "ldap.binddn";
    private final static String SK_LDAP_BIND_PASSWORD = "ldap.bind-password";
    private final static String SK_LDAP_KEYTAB_PATH = "ldap.keytab-path";
    private final static String SK_LDAP_BIND_TYPE = "ldap.bind-type";
    private final static String SK_LDAP_KERBEROS_BIND_PRINCIPAL = "ldap.kerberos-bind-princopal";
    private final static String SK_LDAP_USERS_OU = "ldap.users.ou";
    private final static String SK_LDAP_USERS_OBJECTCLASS = "ldap.users.objectclass";
    private final static String SK_LDAP_USERS_ATTR_USERNAME = "ldap.users.attr-username";
    private final static String SK_LDAP_USERS_ATTR_DISPLAY_NAME = "ldap.users.attr-displayname";
    private final static String SK_LDAP_USERS_ATTR_EMAIL = "ldap.users.attr-email";
    private final static String SK_LDAP_USERS_CUSTOM_FILTER = "ldap.users.search-filter";
    private final static String SK_LDAP_USERS_ENABLE_CUSTOM_FILTER = "ldap.users.enable-custom-filter";
    private final static String SK_LDAP_GROUPS_OU = "ldap.groups.ou";
    private final static String SK_LDAP_GROUPS_ATTR_NAME = "ldap.groups.name";
    private final static String SK_LDAP_GROUPS_ATTR_MEMBER = "ldap.groups.member";
    private final static String SK_LDAP_GROUPS_ATTR_DESCRIPTION = "ldap.groups.description";
    private final static String SK_LDAP_GROUPS_CUSTOM_FILTER = "ldap.groups.search-filter";
    private final static String SK_LDAP_GROUPS_ENABLE_CUSTOM_FILTER = "ldap.groups.enable-custom-filter";
    private final static String SK_LDAP_GROUPS_OBJECTCLASS = "ldap.groups.objectclass";
    private final static String SK_LDAP_CACHE_ENABLED = "ldap.cache.enabled";
    private final static String SK_LDAP_CACHE_TIMEOUT = "ldap.cache.timeout";

    public enum LdapBindType {
        ANONYMOUS("Anonymous"),
        BIND_DN("Bind DN + Password"),
        KEYTAB("Kerberos with Keytab");

        final private String typeStr;

        LdapBindType(String typeStr) {
            this.typeStr = typeStr;
        }

        @Override
        public String toString() {
            return typeStr;
        }
    }

    public LdapSettings() {
    }

    public LdapSettings(Settings settings) {
        enableLdapUserSource = settings.getBoolean(SK_LDAP_ENABLE_USER_SOURCE, false);
        ldapUrls = settings.getList(SK_LDAP_URLS, null)
                .stream()
                .map(urlStr -> new LdapUrl(urlStr))
                .toList();
        baseDn = settings.get(SK_LDAP_BASE_DN, NetUtils.defaultBaseDn());
        bindPassword = settings.get(SK_LDAP_BIND_PASSWORD, "");
        bindType = LdapBindType.valueOf(
                settings.get(SK_LDAP_BIND_TYPE, LdapBindType.BIND_DN.name())
        );
        bindDn = settings.get(SK_LDAP_BIND_DN, NetUtils.defaultBaseDn());
        keytabPath = settings.get(
                SK_LDAP_KEYTAB_PATH,
                FolderFactory.getInstance().getDefaultKeytabPath()
        );
        kerberosBindPricipal = settings.get(
                SK_LDAP_KERBEROS_BIND_PRINCIPAL,
                ""
        );

        usersOu = settings.get(SK_LDAP_USERS_OU, "");
        usersAttrUsername = settings.get(SK_LDAP_USERS_ATTR_USERNAME, "");
        usersAttrDisplayName = settings.get(SK_LDAP_USERS_ATTR_DISPLAY_NAME, "");
        usersAttrEmail = settings.get(SK_LDAP_USERS_ATTR_EMAIL, "");
        usersCustomFilter = settings.get(SK_LDAP_USERS_CUSTOM_FILTER, "");
        usersEnableCustomFilter = settings.getBoolean(SK_LDAP_USERS_ENABLE_CUSTOM_FILTER, false);
        usersObjectClass = settings.get(SK_LDAP_USERS_OBJECTCLASS, "");

        groupsOu = settings.get(SK_LDAP_GROUPS_OU, "");
        groupsAttrMember = settings.get(SK_LDAP_GROUPS_ATTR_MEMBER, "");
        groupsAttrName = settings.get(SK_LDAP_GROUPS_ATTR_NAME, "");
        groupsAttrDescription = settings.get(SK_LDAP_GROUPS_ATTR_DESCRIPTION, "");
        groupsCustomFilter = settings.get(SK_LDAP_GROUPS_CUSTOM_FILTER, "");
        groupsEnableCustomFilter = settings.getBoolean(SK_LDAP_GROUPS_ENABLE_CUSTOM_FILTER, false);
        groupsObjectClass = settings.get(SK_LDAP_GROUPS_OBJECTCLASS, "");

        cacheEnabled = settings.getBoolean(SK_LDAP_CACHE_ENABLED, true);
        cacheTimeOut = settings.getInt(SK_LDAP_CACHE_TIMEOUT, 60);
    }

    public void guessDefaultsFromDns(Settings settings) {
        ldapUrls = findLdapUrls()
                .stream()
                .map(urlStr -> new LdapUrl(urlStr))
                .toList();
        bindDn = NetUtils.defaultBaseDn();
    }

    public void save(Settings settings) {
        settings.put(SK_LDAP_ENABLE_USER_SOURCE, enableLdapUserSource);

        settings.put(
                SK_LDAP_URLS,
                ldapUrls.stream()
                        .map(url -> Objects.toString(url, null))
                        .toList()
        );
        settings.put(SK_LDAP_BASE_DN, baseDn);
        settings.put(SK_LDAP_BIND_PASSWORD, bindPassword);
        settings.put(SK_LDAP_BIND_TYPE, bindType.name());
        settings.put(SK_LDAP_BIND_DN, bindDn);
        settings.put(SK_LDAP_KEYTAB_PATH, keytabPath);
        settings.put(SK_LDAP_KERBEROS_BIND_PRINCIPAL, kerberosBindPricipal);

        settings.put(SK_LDAP_USERS_OU, usersOu);
        settings.put(SK_LDAP_USERS_OBJECTCLASS, usersObjectClass);
        settings.put(SK_LDAP_USERS_ATTR_USERNAME, usersAttrUsername);
        settings.put(SK_LDAP_USERS_ATTR_DISPLAY_NAME, usersAttrDisplayName);
        settings.put(SK_LDAP_USERS_ATTR_EMAIL, usersAttrEmail);
        settings.put(SK_LDAP_USERS_ENABLE_CUSTOM_FILTER, usersEnableCustomFilter);
        settings.put(SK_LDAP_USERS_CUSTOM_FILTER, usersCustomFilter);

        settings.put(SK_LDAP_GROUPS_OU, groupsOu);
        settings.put(SK_LDAP_GROUPS_OBJECTCLASS, groupsObjectClass);
        settings.put(SK_LDAP_GROUPS_ATTR_NAME, groupsAttrName);
        settings.put(SK_LDAP_GROUPS_ATTR_MEMBER, groupsAttrMember);
        settings.put(SK_LDAP_GROUPS_ATTR_DESCRIPTION, groupsAttrDescription);
        settings.put(SK_LDAP_GROUPS_CUSTOM_FILTER, groupsCustomFilter);
        settings.put(SK_LDAP_GROUPS_ENABLE_CUSTOM_FILTER, groupsEnableCustomFilter);

        settings.put(SK_LDAP_CACHE_ENABLED, cacheEnabled);
        settings.put(SK_LDAP_CACHE_TIMEOUT, cacheTimeOut);
    }

    @Value("${arachneConfigDir}")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String arachneConfigDir;

    private boolean enableLdapUserSource;

    List<LdapUrl> ldapUrls;
    private String baseDn;
    private LdapBindType bindType;
    private String bindDn;
    private String bindPassword;
    private String keytabPath;
    private String kerberosBindPricipal;

    private String usersOu;
    private String usersAttrUsername;
    private String usersAttrDisplayName;
    private String usersAttrEmail;
    private String usersCustomFilter;
    private String usersObjectClass;
    private boolean usersEnableCustomFilter;

    private String groupsOu;
    private String groupsAttrName;
    private String groupsAttrMember;
    private String groupsAttrDescription;
    private String groupsCustomFilter;
    private boolean groupsEnableCustomFilter;
    private String groupsObjectClass;

    private boolean cacheEnabled;
    private int cacheTimeOut;

    List<String> findLdapUrls() {
        List<String> ldapServers = new LinkedList<>();
        try {
            for (SrvRecord r : NetUtils.srvLookup("ldap")) {
                ldapServers.add(
                        "%s://%s:%d".formatted(
                                r.getPort() == 636 ? "ldaps" : "ldap",
                                r.getHostname(),
                                r.getPort()
                        )
                );
            }
        } catch (NamingException ex) {
            logger.error("Cannot find ldap SRV record: " + ex.getMessage());
        }

        return ldapServers;
    }

    public LdapTemplate getLdapTemplate() throws Exception {
        logger.info(toString());
        String[] urls = ldapUrls.stream()
                .map(url -> url.toString())
                .toArray(String[]::new);
        LdapTemplate ldapTempl = new LdapTemplate(switch (bindType) {
            case ANONYMOUS -> {
                LdapContextSource ctxSrc = new LdapContextSource();
                ctxSrc.setUrls(urls);
                ctxSrc.setBase(baseDn);
                ctxSrc.setAnonymousReadOnly(true);
                ctxSrc.afterPropertiesSet();

                yield ctxSrc;
            }
            case BIND_DN -> {
                LdapContextSource ctxSrc = new LdapContextSource();
                ctxSrc.setUrls(urls);
                ctxSrc.setBase(baseDn);
                ctxSrc.setUserDn(bindDn);
                ctxSrc.setPassword(bindPassword);
                ctxSrc.afterPropertiesSet();

                yield ctxSrc;
            }
            case KEYTAB -> {
                KerberosLdapContextSource ctxSrc = new KerberosLdapContextSource(
                        ldapUrls.stream()
                                .map(url -> url.toString())
                                .toList(),
                        baseDn
                );
                String krb5Conf = FolderFactory.getInstance().getKrb5ConfPath();
                System.setProperty(
                        "java.security.krb5.conf",
                        krb5Conf
                );

                SunJaasKrb5LoginConfig loginConfig = new SunJaasKrb5LoginConfig();
                loginConfig.setKeyTabLocation(new FileSystemResource(keytabPath));
                loginConfig.setServicePrincipal(kerberosBindPricipal);
                loginConfig.setDebug(true);
                loginConfig.afterPropertiesSet();
                loginConfig.setIsInitiator(true);
                ctxSrc.setLoginConfig(loginConfig);

                Map<String, Object> environment = new HashMap<>();
                environment.put("com.sun.jndi.ldap.connect.timeout", "1000");
                environment.put("com.sun.jndi.ldap.read.timeout", "1000");
                ctxSrc.setBaseEnvironmentProperties(environment);

                ctxSrc.afterPropertiesSet();

                yield ctxSrc;
            }
        });
        ldapTempl.setDefaultTimeLimit(1000);
        return ldapTempl;
    }

    public String getUsersFilter(String username) {
        return getUsersFilter()
                .replace("{username}", username);
    }

    public String getUsersFilter() {
        if (usersEnableCustomFilter) {
            return usersCustomFilter;
        } else {
            return "(&(objectclass=%s)(%s={username}))"
                    .formatted(usersObjectClass, usersAttrUsername);
        }
    }

    public String getGroupsFilter(String groupname) {
        return getGroupsFilter()
                .replace("{groupname}", groupname);
    }

    public String getGroupsFilter() {
        if (groupsEnableCustomFilter) {
            return groupsCustomFilter;
        } else {
            return "(&(objectclass=%s)(%s={groupname}))"
                    .formatted(groupsObjectClass, groupsAttrName);
        }
    }

    public List<ArachneUser> findUsers(String username, int max) {
        LdapTemplate ldap;
        try {
            ldap = getLdapTemplate();
        } catch (Exception ex) {
            return null;
        }
        String filter = getUsersFilter(username);
        logger.info("LDAP filter: " + filter);
        SearchControls sc = new SearchControls();
        sc.setCountLimit(max);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(
                new String[]{
                    "dn",
                    getUsersAttrUsername(),
                    getUsersAttrDisplayName(),
                    getUsersAttrEmail()
                }
        );
        var result = ldap.search(
                getUsersOu(),
                filter,
                sc,
                new AbstractContextMapper<ArachneUser>() {
            @Override
            protected ArachneUser doMapFromContext(DirContextOperations dco) {
                logger.info("Found: " + dco.toString());
                ArachneUser ldapUser = ArachneUser.builder()
                        .externalId(dco.getDn().toString())
                        .externalProvider(LdapUserSource.getName())
                        .username(dco.getStringAttribute(getUsersAttrUsername()))
                        .displayName(dco.getStringAttribute(getUsersAttrDisplayName()))
                        .email(dco.getStringAttribute(getUsersAttrEmail()))
                        .build();
                return ldapUser;
            }
        });

        return result;
    }

    public ArachneUser getUser(String username) {
        List<ArachneUser> users = findUsers(username, 1);
        if (users.isEmpty()) {
            return null;
        }
        return users.get(0);
    }

    public List<LdapGroup> findGroups(String groupName, int max) {
        LdapTemplate ldap;
        try {
            ldap = getLdapTemplate();
        } catch (Exception ex) {
            return null;
        }

        String filter = getGroupsFilter(groupName);
        logger.info("LDAP filter: " + filter);
        SearchControls sc = new SearchControls();
        sc.setCountLimit(max);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(
                new String[]{
                    "dn",
                    getGroupsAttrName(),
                    getGroupsAttrDescription(),
                    getGroupsAttrMember()
                }
        );

        var groups = ldap.search(
                getGroupsOu(),
                filter,
                sc,
                new AbstractContextMapper<LdapGroup>() {
            @Override
            protected LdapGroup doMapFromContext(DirContextOperations dco) {
                LdapGroup ldapGroup = new LdapGroup();
                ldapGroup.setDn(
                        "%s,%s"
                                .formatted(
                                        dco.getDn().toString(),
                                        getBaseDn()
                                )
                );
                ldapGroup.setName(
                        dco.getStringAttribute(getGroupsAttrName())
                );
                ldapGroup.setDescription(
                        dco.getStringAttribute(getGroupsAttrDescription())
                );
                ldapGroup.setMembers(
                        dco.getStringAttributes(getGroupsAttrMember())
                );
                logger.info("Found: " + ldapGroup);
                return ldapGroup;

            }
        });

        return groups;
    }

    public LdapGroup getGroup(String groupname) {
        List<LdapGroup> groups = findGroups(groupname, 1);
        if (groups.isEmpty()) {
            return null;
        }
        return groups.get(0);
    }
}
