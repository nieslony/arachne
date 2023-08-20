/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.Role;
import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.openvpnadmin.beans.RoleRuleFactory;
import at.nieslony.openvpnadmin.beans.RoleRuleFactoryCollection;
import at.nieslony.openvpnadmin.beans.Roles;
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

    @ManagedProperty(value = "#{roles}")
    Roles roles;
    public void setRoles(Roles rb) {
        roles = rb;
    }

    @ManagedProperty(value = "#{roleRuleFactoryCollection}")
    RoleRuleFactoryCollection roleRuleFactoryCollection;
    public void setRoleRuleFactoryCollection(RoleRuleFactoryCollection rrfc) {
        roleRuleFactoryCollection = rrfc;
    }

    private String addRuleToRole;
    private String addRuleType;
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
        logger.info(String.format("Trying to complete %s", pattern));

        if (pattern == null)
            return null;

        RoleRuleFactory factory = roleRuleFactoryCollection.getFactory(addRuleType);
        if (factory == null) {
            logger.warning(String.format("rule type %s doesn't exist", addRuleType));
            return new LinkedList<>();
        }
        return factory.completeValue(pattern);
    }

    public void onAddRule(Role role) {
        addRuleToRole = role.getName();

        RequestContext.getCurrentInstance().execute("PF('dlgAddRule').show();");
    }

    public void onRemoveRuleFromRole(Role role, RoleRule rule) {
        roles.removeRuleFromRole(role, rule);
    }

    public void onAddRuleOk(Role role) {
        logger.info(String.format("Adding rule (%s=%s) to role %s",
                addRuleType, addRuleValue, addRuleToRole));

        RoleRule rule = roleRuleFactoryCollection.createRoleRule(addRuleType, addRuleValue);
        if (rule != null) {
            roles.addRule(addRuleToRole, addRuleType, addRuleValue);
        }
        else
            logger.warning("Unable to create rule");

        RequestContext.getCurrentInstance().execute("PF('dlgAddRule').hide();");
    }
}
