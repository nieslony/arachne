/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans.firewallzone;

import at.nieslony.openvpnadmin.RoleRule;
import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author claas
 */
public class Who implements Serializable {
    RoleRule roleRule = null;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Who other = (Who) obj;
        if (!Objects.equals(this.roleRule, other.roleRule)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return roleRule.hashCode();
    }

    public String getAsString() {
        return String.format("%s %s",
                roleRule.getRoleDescription(),
                roleRule.getValue());
    }

    public RoleRule getRoleRule() {
        return roleRule;
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
