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
import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.utils.net.NetUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

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

    public LdapSettings() {
    }

    public void guessDefaultsFromDns(Settings settings) {
        ldapUrls = LdapController.getInstance().findLdapUrls()
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

    public String getUsersFilter(String username) {
        return getUsersFilter()
                .replace("{username}", username);
    }

    public String getUsersFilter() {
        if (usersEnableCustomFilter) {
            return usersCustomFilter;
        } else if (usersObjectClass == null) {
            return null;
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
        } else if (groupsObjectClass == null) {
            return null;
        } else {
            return "(&(objectclass=%s)(%s={groupname}))"
                    .formatted(groupsObjectClass, groupsAttrName);
        }
    }

    @JsonIgnore
    public boolean isValid() {
        return ldapUrls != null
                && !ldapUrls.isEmpty()
                && getUsersFilter() != null
                && getGroupsFilter() != null;
    }
}
