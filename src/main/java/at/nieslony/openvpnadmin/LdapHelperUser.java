/*
 * Copyright (C) 2018 Claas Nieslony
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

package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.base.LdapSettingsBase;

/**
 *
 * @author claas
 */
public interface LdapHelperUser {
    public LdapSettingsBase.LdapAuthType getAuthType();
    public String getLdapServer();
    public String getLdapBaseDn();
    public Integer getLdapPort();
    public String getLdapDnsDomain();
    public LdapSettingsBase.LdapServerLookupMethod getLdapServereLookupMethod();
    public String getSecurityPrincipal();
    public String getSecurityCredentials();
    public String getKeytabFile();
    public String getKerberosPrincipal();
    public String getLoginContextName();

    public String getOuUsers();
    public String getAttrUsername();
    public String getAttrFullName();
    public String getAttrGivenName();
    public String getAttrSurname();
    public String getAttrEmail();
    public String getObjectClassUser();
    public String getCustomUserSearchFilter();
    public Boolean getUseCustomUserSearchFilter();

    public String getOuGroups();
    public String getObjectClassGroup();
    public String getAttrGroupName();
    public String getAttrGroupDescription();
    public String getAttrGroupMemberDn();
    public String getAttrGroupMemberUid();
    public String getCustomGroupSearchFilter();
    public Boolean getUseCustomGroupSearchFilter();
    public String getGroupSearchFilter(String group);
    public LdapSettingsBase.MemberAttrType getMemberAttrType();

    public boolean auth(String dn, String password);
}
