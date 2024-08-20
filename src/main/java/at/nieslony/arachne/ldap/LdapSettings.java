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

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.SrvRecord;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
public class LdapSettings extends AbstractSettingsGroup {

    private static final Logger logger = LoggerFactory.getLogger(LdapSettings.class);

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

    private class GroupContextMapper extends AbstractContextMapper<LdapGroup> {

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
    }

    private class UserContextMapper extends AbstractContextMapper<UserModel> {

        @Override
        protected UserModel doMapFromContext(DirContextOperations dco) {
            logger.info("Found: " + dco.toString());
            UserModel ldapUser = UserModel.builder()
                    .externalId("%s,%s".formatted(
                            dco.getDn().toString(),
                            getBaseDn()
                    ))
                    .externalProvider(LdapUserSource.getName())
                    .username(dco.getStringAttribute(getUsersAttrUsername()))
                    .displayName(dco.getStringAttribute(getUsersAttrDisplayName()))
                    .email(dco.getStringAttribute(getUsersAttrEmail()))
                    .build();
            return ldapUser;
        }
    }

    public LdapSettings() {
    }

    public void guessDefaultsFromDns(Settings settings) {
        ldapUrls = findLdapUrls()
                .stream()
                .map(urlStr -> new LdapUrl(urlStr))
                .toList();
        bindDn = NetUtils.defaultBaseDn();
    }

    @Value("${arachneConfigDir}")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String arachneConfigDir;

    private boolean enableLdapUserSource = false;

    List<LdapUrl> ldapUrls = new LinkedList<>();
    private String baseDn = NetUtils.defaultBaseDn();
    private LdapBindType bindType = LdapBindType.BIND_DN;
    private String bindDn = NetUtils.defaultBaseDn();
    private String bindPassword = "";
    private String keytabPath = FolderFactory.getInstance().getDefaultKeytabPath();
    private String kerberosBindPricipal = "";

    private String usersOu = "";
    private String usersAttrUsername = "";
    private String usersAttrDisplayName = "";
    private String usersAttrEmail = "";
    private String usersCustomFilter = "";
    private String usersObjectClass = "";
    private boolean usersEnableCustomFilter = false;

    private String groupsOu = "";
    private String groupsAttrName = "";
    private String groupsAttrMember = "";
    private String groupsAttrDescription = "";
    private String groupsCustomFilter = "";
    private boolean groupsEnableCustomFilter = false;
    private String groupsObjectClass = "";

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

    @JsonIgnore
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

    public List<UserModel> findUsers(String username, int max) {
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
                new UserContextMapper()
        );

        return result;
    }

    public List<String> findUsersPretty(String pattern, int max) {
        LdapTemplate ldap;
        try {
            ldap = getLdapTemplate();
        } catch (Exception ex) {
            logger.error("Cannot getLdapTemplate: " + ex.getMessage());
            return null;
        }
        String filter
                = "(&(objectclass=%s)(|(%s=%s)(%s=%s)))"
                        .formatted(
                                getUsersObjectClass(),
                                getUsersAttrUsername(),
                                pattern,
                                getUsersAttrDisplayName(),
                                pattern
                        );
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
        return ldap
                .search(
                        getUsersOu(),
                        filter,
                        sc,
                        new UserContextMapper()
                )
                .stream()
                .map((user) -> {
                    String displayName = user.getDisplayName();
                    if (displayName != null && !displayName.isEmpty()) {
                        return "%s (%s)"
                                .formatted(
                                        user.getUsername(),
                                        displayName
                                );
                    } else {
                        return user.getUsername();
                    }
                })
                .sorted()
                .toList();
    }

    public List<String> findGroupsPretty(String pattern, int max) {
        LdapTemplate ldap;
        try {
            ldap = getLdapTemplate();
        } catch (Exception ex) {
            logger.error("Cannot getLdapTemplate: " + ex.getMessage());
            return null;
        }
        String filter
                = "(&(objectclass=%s)(|(%s=%s)(%s=%s)))"
                        .formatted(
                                getGroupsObjectClass(),
                                getGroupsAttrName(),
                                pattern,
                                getGroupsAttrDescription(),
                                pattern
                        );
        logger.info("LDAP filter: " + filter);
        SearchControls sc = new SearchControls();
        sc.setCountLimit(max);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(
                new String[]{
                    "dn",
                    getGroupsAttrName(),
                    getGroupsAttrDescription()
                }
        );
        return ldap
                .search(
                        getGroupsOu(),
                        filter,
                        sc,
                        new GroupContextMapper()
                )
                .stream()
                .map((group) -> {
                    String description = group.getDescription();
                    if (description != null && !description.isEmpty()) {
                        return "%s (%s)"
                                .formatted(
                                        group.getName(),
                                        description
                                );
                    } else {
                        return group.getName();
                    }
                })
                .sorted()
                .toList();
    }

    @JsonIgnore
    public UserModel getUser(String username) {
        List<UserModel> users = findUsers(username, 1);
        if (users == null || users.isEmpty()) {
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
                new GroupContextMapper()
        );

        return groups;
    }

    @JsonIgnore
    public LdapGroup getGroup(String groupname) {
        List<LdapGroup> groups = findGroups(groupname, 1);
        if (groups.isEmpty()) {
            return null;
        }
        return groups.get(0);
    }
}
