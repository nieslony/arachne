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
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.PrimeFaces;

/**
 *
 * @author claas
 */
@ViewScoped
@Named
public class EditRoles implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    Roles roles;
    public void setRoles(Roles rb) {
        roles = rb;
    }

    @Inject
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

        PrimeFaces.current().executeScript("PF('dlgAddRule').show();");
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

        PrimeFaces.current().executeScript("PF('dlgAddRule').hide();");
    }
}
