/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans.firewallzone;

import at.nieslony.openvpnadmin.RoleRule;
import java.io.Serializable;

/**
 *
 * @author claas
 */
public class Who implements Serializable {
    RoleRule roleRule = null;

    public String getAsString() {
        return String.format("%s %s",
                roleRule.getRoleDescription(),
                roleRule.getValue());
    }

    public void setTypeAndValue(RoleRule rr) {
        roleRule = rr;
    }

    public String getWhoValue() {
        return roleRule == null ? "" : roleRule.getValue();
    }

    public String getWhoType() {
        return roleRule == null ? "" : roleRule.getRoleType();
    }
}
