/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.LdapSettings;
import java.util.List;

/**
 *
 * @author claas
 */
public interface RoleRule {
    public boolean isAssumedByUser(String userName);
    public String getRoleType();

    public String getValue();
    public void setValue(String s);

    public List<String> completeValue(String value, LdapSettings ldapSettings);
}
