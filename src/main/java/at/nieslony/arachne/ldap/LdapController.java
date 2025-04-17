/*
 * Copyright (C) 2025 claas
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

import static at.nieslony.arachne.ldap.LdapSettings.LdapBindType.ANONYMOUS;
import static at.nieslony.arachne.ldap.LdapSettings.LdapBindType.BIND_DN;
import static at.nieslony.arachne.ldap.LdapSettings.LdapBindType.KEYTAB;
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.SrvRecord;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.repository.cdi.Eager;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.kerberos.client.config.SunJaasKrb5LoginConfig;
import org.springframework.security.kerberos.client.ldap.KerberosLdapContextSource;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;

/**
 *
 * @author claas
 */
@Controller
@Eager
@Log4j2
public class LdapController {

    private static LdapController ldapController;

    public LdapController() {
        ldapController = this;
    }

    public static LdapController getInstance() {
        return ldapController;
    }

    private class UserContextMapper extends AbstractContextMapper<UserModel> {

        final LdapSettings ldapSettings;

        UserContextMapper(LdapSettings ldapSettings) {
            this.ldapSettings = ldapSettings;
        }

        @Override
        protected UserModel doMapFromContext(DirContextOperations dco) {
            log.info("Found: " + dco.toString());
            UserModel ldapUser = UserModel.builder()
                    .externalId("%s,%s".formatted(
                            dco.getDn().toString(),
                            ldapSettings.getBaseDn()
                    ))
                    .externalProvider(LdapUserSource.getName())
                    .username(dco.getStringAttribute(ldapSettings.getUsersAttrUsername()))
                    .displayName(dco.getStringAttribute(ldapSettings.getUsersAttrDisplayName()))
                    .email(dco.getStringAttribute(ldapSettings.getUsersAttrEmail()))
                    .build();
            return ldapUser;
        }
    }

    private class GroupContextMapper extends AbstractContextMapper<LdapGroup> {

        final LdapSettings ldapSettings;

        GroupContextMapper(LdapSettings ldapSettings) {
            this.ldapSettings = ldapSettings;
        }

        @Override
        protected LdapGroup doMapFromContext(DirContextOperations dco) {
            LdapGroup ldapGroup = new LdapGroup();
            ldapGroup.setDn(
                    "%s,%s"
                            .formatted(
                                    dco.getDn().toString(),
                                    ldapSettings.getBaseDn()
                            )
            );
            ldapGroup.setName(
                    dco.getStringAttribute(ldapSettings.getGroupsAttrName())
            );
            ldapGroup.setDescription(
                    dco.getStringAttribute(ldapSettings.getGroupsAttrDescription())
            );
            ldapGroup.setMembers(
                    dco.getStringAttributes(ldapSettings.getGroupsAttrMember())
            );
            log.info("Found: " + ldapGroup);
            return ldapGroup;
        }
    }

    public record PrettyResult(String name, String description) {

        @Override
        public String toString() {
            if (ObjectUtils.isEmpty(description)) {
                return name;
            }
            return "%s (%s)".formatted(name, description);
        }
    }

    public List<String> findLdapUrls() {
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
            log.error("Cannot find ldap SRV record: " + ex.getMessage());
        }

        return ldapServers;
    }

    public LdapTemplate getLdapTemplate(LdapSettings ldapSettings) throws Exception {
        log.info(toString());
        String[] urls = ldapSettings.getLdapUrls().stream()
                .map(url -> url.toString())
                .toArray(String[]::new);
        LdapTemplate ldapTempl = new LdapTemplate(switch (ldapSettings.getBindType()) {
            case ANONYMOUS -> {
                LdapContextSource ctxSrc = new LdapContextSource();
                ctxSrc.setUrls(urls);
                ctxSrc.setBase(ldapSettings.getBaseDn());
                ctxSrc.setAnonymousReadOnly(true);
                ctxSrc.afterPropertiesSet();

                yield ctxSrc;
            }
            case BIND_DN -> {
                LdapContextSource ctxSrc = new LdapContextSource();
                ctxSrc.setUrls(urls);
                ctxSrc.setBase(ldapSettings.getBaseDn());
                ctxSrc.setUserDn(ldapSettings.getBindDn());
                ctxSrc.setPassword(ldapSettings.getBindPassword());
                ctxSrc.afterPropertiesSet();

                yield ctxSrc;
            }
            case KEYTAB -> {
                KerberosLdapContextSource ctxSrc = new KerberosLdapContextSource(
                        ldapSettings.getLdapUrls().stream()
                                .map(url -> url.toString())
                                .toList(),
                        ldapSettings.getBaseDn()
                );
                String krb5Conf = FolderFactory.getInstance().getKrb5ConfPath();
                System.setProperty(
                        "java.security.krb5.conf",
                        krb5Conf
                );

                SunJaasKrb5LoginConfig loginConfig = new SunJaasKrb5LoginConfig();
                loginConfig.setKeyTabLocation(
                        new FileSystemResource(ldapSettings.getKeytabPath())
                );
                loginConfig.setServicePrincipal(
                        ldapSettings.getKerberosBindPricipal()
                );
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

    public List<UserModel> findUsers(LdapSettings ldapSettings, String username, int max) {
        LdapTemplate ldap;
        try {
            ldap = getLdapTemplate(ldapSettings);
        } catch (Exception ex) {
            return null;
        }
        String filter = ldapSettings.getUsersFilter(username);
        log.info("LDAP filter: " + filter);
        SearchControls sc = new SearchControls();
        if (max != -1) {
            sc.setCountLimit(max);
        }
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(
                new String[]{
                    "dn",
                    ldapSettings.getUsersAttrUsername(),
                    ldapSettings.getUsersAttrDisplayName(),
                    ldapSettings.getUsersAttrEmail()
                }
        );
        var result = ldap.search(
                ldapSettings.getUsersOu(),
                filter,
                sc,
                new UserContextMapper(ldapSettings)
        );

        return result;
    }

    public List<PrettyResult> findUsersPretty(LdapSettings ldapSettings, String pattern, int max) {
        LdapTemplate ldap;
        try {
            ldap = getLdapTemplate(ldapSettings);
        } catch (Exception ex) {
            log.error("Cannot getLdapTemplate: " + ex.getMessage());
            return null;
        }
        String filter
                = "(&(objectclass=%s)(|(%s=%s)(%s=%s)))"
                        .formatted(
                                ldapSettings.getUsersObjectClass(),
                                ldapSettings.getUsersAttrUsername(),
                                pattern,
                                ldapSettings.getUsersAttrDisplayName(),
                                pattern
                        );
        log.info("LDAP filter: " + filter);
        SearchControls sc = new SearchControls();
        sc.setCountLimit(max);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(
                new String[]{
                    "dn",
                    ldapSettings.getUsersAttrUsername(),
                    ldapSettings.getUsersAttrDisplayName(),
                    ldapSettings.getUsersAttrEmail()
                }
        );
        return ldap
                .search(
                        ldapSettings.getUsersOu(),
                        filter,
                        sc,
                        new UserContextMapper(ldapSettings)
                )
                .stream()
                .map(
                        (user) -> new PrettyResult(
                                user.getUsername(),
                                user.getDisplayName()
                        )
                )
                .sorted((u1, u2) -> u1.name.compareTo(u2.name))
                .toList();
    }

    public UserModel getUser(LdapSettings ldapSettings, String username) {
        List<UserModel> users = findUsers(ldapSettings, username, 1);
        if (users == null || users.isEmpty()) {
            return null;
        }
        return users.get(0);
    }

    public List<LdapGroup> findGroups(LdapSettings ldapSettings, String groupName, int max) {
        LdapTemplate ldap;
        try {
            ldap = getLdapTemplate(ldapSettings);
        } catch (Exception ex) {
            return null;
        }

        String filter = ldapSettings.getGroupsFilter(groupName);
        log.info("LDAP filter: " + filter);
        SearchControls sc = new SearchControls();
        sc.setCountLimit(max);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(
                new String[]{
                    "dn",
                    ldapSettings.getGroupsAttrName(),
                    ldapSettings.getGroupsAttrDescription(),
                    ldapSettings.getGroupsAttrMember()
                }
        );

        var groups = ldap.search(
                ldapSettings.getGroupsOu(),
                filter,
                sc,
                new GroupContextMapper(ldapSettings)
        );

        return groups;
    }

    public List<PrettyResult> findGroupsPretty(LdapSettings ldapSettings, String pattern, int max) {
        LdapTemplate ldap;
        try {
            ldap = getLdapTemplate(ldapSettings);
        } catch (Exception ex) {
            log.error("Cannot getLdapTemplate: " + ex.getMessage());
            return null;
        }
        String filter
                = "(&(objectclass=%s)(|(%s=%s)(%s=%s)))"
                        .formatted(
                                ldapSettings.getGroupsObjectClass(),
                                ldapSettings.getGroupsAttrName(),
                                pattern,
                                ldapSettings.getGroupsAttrDescription(),
                                pattern
                        );
        log.info("LDAP filter: " + filter);
        SearchControls sc = new SearchControls();
        sc.setCountLimit(max);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(
                new String[]{
                    "dn",
                    ldapSettings.getGroupsAttrName(),
                    ldapSettings.getGroupsAttrDescription()
                }
        );
        return ldap
                .search(
                        ldapSettings.getGroupsOu(),
                        filter,
                        sc,
                        new GroupContextMapper(ldapSettings)
                )
                .stream()
                .map(
                        (group) -> new PrettyResult(
                                group.getName(),
                                group.getDescription()
                        )
                )
                .sorted((g1, g2) -> g1.name.compareTo(g2.name))
                .toList();
    }

    public LdapGroup getGroup(LdapSettings ldapSettings, String groupname) {
        List<LdapGroup> groups = findGroups(ldapSettings, groupname, 1);
        if (groups.isEmpty()) {
            return null;
        }
        return groups.get(0);
    }
}
