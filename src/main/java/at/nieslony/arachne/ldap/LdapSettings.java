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

import at.nieslony.arachne.FolderFactory;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.NetUtils;
import at.nieslony.arachne.utils.SrvRecord;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.naming.NamingException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
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

    private final static String SK_LDAP_URLS = "ldap.urls";
    private final static String SK_LDAP_BASE_DN = "ldap.base-dn";
    private final static String SK_LDAP_BIND_DN = "ldap.binddn";
    private final static String SK_LDAP_BIND_PASSWORD = "ldap.bind-password";
    private final static String SK_LDAP_KEYTAB_PATH = "ldap.keytab-path";
    private final static String SK_LDAP_BIND_TYPE = "ldap.bind-type";
    private final static String SK_LDAP_KERBEROS_BIND_PRINCIPAL = "ldap.kerberos-bind-princopal";

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
        ldapUrls = settings.getList(SK_LDAP_URLS, findLdapUrls())
                .stream()
                .map(urlStr -> new LdapUrl(urlStr))
                .toList();
        baseDn = settings.get(SK_LDAP_BASE_DN, NetUtils.defaultBaseDn());
        bindPassword = settings.get(SK_LDAP_BIND_PASSWORD, "");
        bindType = LdapBindType.valueOf(
                settings.get(
                        SK_LDAP_BIND_TYPE,
                        LdapBindType.BIND_DN.name())
        );
        bindDn = settings.get(SK_LDAP_BIND_DN, NetUtils.defaultBaseDn());
        keytabPath = settings.get(SK_LDAP_KEYTAB_PATH, "");
        kerberosBindPricipal = settings.get(
                SK_LDAP_KERBEROS_BIND_PRINCIPAL,
                "HTTP/" + NetUtils.myHostname()
        );
    }

    public void save(Settings settings) {
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
    }

    @Value("${arachneConfigDir}")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String arachneConfigDir;

    List<LdapUrl> ldapUrls;
    private String baseDn;
    private LdapBindType bindType;
    private String bindDn;
    private String bindPassword;
    private String keytabPath;
    private String kerberosBindPricipal;

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

    public LdapContextSource getLdapContextSource() throws Exception {
        logger.info(toString());
        String[] urls = ldapUrls.stream()
                .map(url -> url.toString())
                .toArray(String[]::new);
        return switch (bindType) {
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
                ctxSrc.afterPropertiesSet();

                yield ctxSrc;
            }
        };
    }
}
