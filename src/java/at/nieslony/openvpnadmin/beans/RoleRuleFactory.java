/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.RoleRule;
import java.util.List;

/**
 *
 * @author claas
 */
public interface RoleRuleFactory {
     public RoleRule createRule(String username);
     public String getRoleRuleName();
     public String getDescriptionString();
     public List<String> completeValue(String userPattern);
}
