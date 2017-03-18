/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.base.LdapSettingsBase;

/**
 *
 * @author claas
 */
public interface LdapHelperUser {
    public String getAuthType();
    public String getProviderUrl();
    public String getSecurityPrincipal();
    public String getSecurityCredentials();

    public String getOuUsers();
    public String getAttrUsername();
    public String getAttrFullName();
    public String getAttrGivenName();
    public String getAttrSurname();
    public String getAttrEmail();
    public String getObjectClassUser();

    public String getOuGroups();
    public String getObjectClassGroup();
    public String getAttrGroupName();
    public String getAttrGroupDescription();
    public String getAttrGroupMemberDn();
    public String getAttrGroupMemberUid();
    public LdapSettingsBase.MemberAttrType getMemberAttrType();
}
