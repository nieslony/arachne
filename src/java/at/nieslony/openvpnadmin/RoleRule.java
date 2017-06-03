/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.RoleRuleFactory;
import java.io.Serializable;

/**
 *
 * @author claas
 */
abstract public class RoleRule
        implements Serializable
{
    private String value;
    private RoleRuleFactory factory;

    public RoleRule() {
    }

    public void init (RoleRuleFactory factory, String value) {
        this.value = value;
        this.factory = factory;
    }

    abstract public boolean isAssumedByUser(AbstractUser user);

    public String getRoleType() {
        return factory.getRoleRuleName();
    }

    public String getRoleDescription() {
        return factory.getDescriptionString();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
