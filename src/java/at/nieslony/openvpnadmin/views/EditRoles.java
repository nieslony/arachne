/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.Role;
import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.openvpnadmin.beans.Roles;
import at.nieslony.openvpnadmin.beans.LdapSettings;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.primefaces.context.RequestContext;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class EditRoles implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{ldapSettings}")
    LdapSettings ldapSettings;
    public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    @ManagedProperty(value = "#{roles}")
    Roles roles;
    public void setRoles(Roles rb) {
        roles = rb;
    }

    private String addRuleToRole;
    private String addRuleType = "at.nieslony.openvpnadmin.RoleRuleIsUser";
    private String addRuleValue;

    /**
     * Creates a new instance of RolesBean
     */
    public EditRoles() {
    }

    public List<Role> getAllRoles() {
        List<Role> r = roles.getRoles();
        logger.info(String.format("%d roles available", r.size()));
        return roles.getRoles();
    }

    public String getAddRuleToRole() {
        return addRuleToRole;
    }

    public String getAddRoleRuleType() {
        return addRuleType;
    }

    public void setAddRoleRuleType(String type) {
        addRuleType = type;
    }

    public String getAddRoleRuleValue() {
        return addRuleValue;
    }

    public void setAddRoleRuleValue(String value) {
        if (value != null)
            addRuleValue = value.replaceFirst(" \\(.*\\)", "");
    }

    public List<String> onCompleteRoleValue(String pattern) {
        if (pattern == null)
            return null;

        RoleRule rule = null;
        List<String> values = new LinkedList<>();

        try {
            rule = (RoleRule) Class.forName(getAddRoleRuleType()).newInstance();
        }
        catch (InstantiationException | IllegalAccessException |
                ClassNotFoundException | SecurityException ex) {
            logger.severe(String.format("Cannot create rule object: %s",
                    ex.getMessage()));
        }

        if (rule != null) {
            values.addAll(rule.completeValue(pattern, ldapSettings));
        }
        else {
            logger.info("No rule object");
        }

        return values;
    }

    public void onAddRule(Role role) {
        addRuleToRole = role.getName();

        RequestContext.getCurrentInstance().execute("PF('dlgAddRule').show();");
    }

    public void onRemoveRuleFromRole(Role role, RoleRule rule) {
        roles.removeRuleFromRole(role, rule);
        roles.save();
    }

    public void onAddRuleOk(Role role) {
        logger.info(String.format("Adding rule (%s=%s) to role %s",
                addRuleType, addRuleValue, addRuleToRole));
        try {
            RoleRule rule = (RoleRule) Class.forName(addRuleType).newInstance();
            rule.setValue(addRuleValue);
            roles.addRule(addRuleToRole, rule);
            roles.save();

            logger.info("Add rule: OK");
        }
        catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
            logger.severe(String.format("Cannot add rule to role: %s",
                    ex.getMessage()));
        }

        RequestContext.getCurrentInstance().execute("PF('dlgAddRule').hide();");
    }

}
