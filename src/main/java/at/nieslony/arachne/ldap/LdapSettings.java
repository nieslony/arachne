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

import at.nieslony.arachne.NetUtils;
import at.nieslony.arachne.settings.Settings;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class LdapSettings {

    private final static String SK_LDAP_PROTOCOL = "ldap.protocol";
    private final static String SK_LDAP_HOST = "ldap.host";
    private final static String SK_LDAP_PORT = "ldap.port";
    private final static String SK_LDAP_BASE_DN = "ldap.basedn";
    private final static String SK_LDAP_BIND_DN = "ldap.binddn";
    private final static String SK_LDAP_BIND_PASSWORD = "ldap.bind-password";
    private final static String SK_LDAP_KEYTAB_PATH = "ldap.keytab-path";
    private final static String SK_LDAP_BIND_TYPE = "ldap.bind-type";

    public enum LdapProtocol {
        LDAP("ldap", 389),
        LDAPS("ldaps", 636);

        final private String name;
        final private int port;

        LdapProtocol(String name, int port) {
            this.name = name;
            this.port = port;
        }

        @Override
        public String toString() {
            return name;
        }

        public int getPort() {
            return port;
        }
    }

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
        protocol = LdapProtocol.valueOf(
                settings.get(
                        SK_LDAP_PROTOCOL,
                        LdapProtocol.LDAP.name())
        );
        try {
            host = settings.get(SK_LDAP_HOST, NetUtils.srvLookup("ldap"));
        } catch (NamingException ex) {
            host = "ldap." + NetUtils.myDomain();
        }
        port = settings.getInt(SK_LDAP_PORT, 389);
        baseDn = settings.get(SK_LDAP_BASE_DN, NetUtils.defaultBaseDn());
        bindPassword = settings.get(SK_LDAP_BIND_PASSWORD, "");
        bindType = LdapBindType.valueOf(
                settings.get(
                        SK_LDAP_BIND_TYPE,
                        LdapBindType.BIND_DN.name())
        );
        bindDn = settings.get(SK_LDAP_BIND_DN, NetUtils.defaultBaseDn());
        keytabPath = settings.get(SK_LDAP_KEYTAB_PATH, "");
    }

    public void save(Settings settings) {
        settings.put(SK_LDAP_PROTOCOL, protocol.name);
        settings.put(SK_LDAP_HOST, host);
        settings.put(SK_LDAP_PORT, port);
        settings.put(SK_LDAP_BASE_DN, baseDn);
        settings.put(SK_LDAP_BIND_PASSWORD, bindPassword);
        settings.put(SK_LDAP_BIND_TYPE, bindType.name());
        settings.put(SK_LDAP_BIND_DN, bindDn);
        settings.put(SK_LDAP_KEYTAB_PATH, keytabPath);
    }

    private LdapProtocol protocol;
    private String host;
    private int port;
    private String baseDn;
    private LdapBindType bindType;
    private String bindDn;
    private String bindPassword;
    private String keytabPath;
}
