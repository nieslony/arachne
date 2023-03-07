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
import at.nieslony.arachne.settings.SettingsModel;
import at.nieslony.arachne.settings.SettingsRepository;
import java.util.Optional;
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

    public LdapSettings(SettingsRepository settingsRepository) {
        Optional<SettingsModel> setting;
        String myDomain = NetUtils.myDomain();

        setting = settingsRepository.findBySetting(SK_LDAP_PROTOCOL);
        protocol = setting.isPresent()
                ? LdapProtocol.valueOf(setting.get().getContent())
                : LdapProtocol.LDAP;

        setting = settingsRepository.findBySetting(SK_LDAP_HOST);
        try {
            host = setting.isPresent()
                    ? setting.get().getContent()
                    : NetUtils.srvLookup("ldap");
        } catch (NamingException ex) {
            host = "ldap." + myDomain;
        }

        setting = settingsRepository.findBySetting(SK_LDAP_PORT);
        port = setting.isPresent() ? setting.get().getIntContent() : 389;

        setting = settingsRepository.findBySetting(SK_LDAP_BASE_DN);
        baseDn = setting.isPresent()
                ? setting.get().getContent()
                : NetUtils.defaultBaseDn();

        setting = settingsRepository.findBySetting(SK_LDAP_BIND_TYPE);
        bindType = setting.isPresent()
                ? LdapBindType.valueOf(setting.get().getContent())
                : LdapBindType.BIND_DN;

        setting = settingsRepository.findBySetting(SK_LDAP_BASE_DN);
        bindDn = setting.isPresent() ? setting.get().getContent() : "";

        setting = settingsRepository.findBySetting(SK_LDAP_KEYTAB_PATH);
        keytabPath = setting.isPresent() ? setting.get().getContent() : "";
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
