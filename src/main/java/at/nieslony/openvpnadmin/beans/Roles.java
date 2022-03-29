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

package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.Role;
import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.openvpnadmin.errorhandling.RuleAlreadyExists;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
@ApplicationScoped
@Named
public class Roles implements Serializable {
    private final Map<String, Role> roles = new HashMap<>();
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    private DatabaseSettings databaseSettings;

    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }

    @Inject
    RoleRuleFactoryCollection roleRuleFactoryCollection;

    public void setRoleRuleFactoryCollection(RoleRuleFactoryCollection rrfc) {
        roleRuleFactoryCollection = rrfc;
    }

    public RoleRuleFactoryCollection getRoleRuleFactoryCollection() {
        return roleRuleFactoryCollection;
    }

    /**
     * Creates a new instance of Roles
     */
    public Roles() {
    }

    public Connection getDatabaseConnection()
            throws ClassNotFoundException, SQLException
    {
        return databaseSettings.getDatabaseConnection();
    }

    @PostConstruct
    public void init() {
        load();
    }

    public void load() {
        logger.info("Loading roles from database");
        roles.clear();
        try {
            Connection con = getDatabaseConnection();
            Statement stm = con.createStatement();
            String sql = "SELECT * FROM roles;";
            ResultSet result = stm.executeQuery(sql);
            while (result.next()) {
                String roleName = result.getString("rolename");
                String id = result.getString("id");
                Role role = new Role();
                role.init(this, id, roleName);
                roles.put(roleName, role);
            }
        }
        catch (ClassNotFoundException | SQLException ex) {
            logger.severe(String.format("Cannot load roles: %s", ex.getMessage()));
        }
    }

    public boolean hasUserRole(AbstractUser user, String rolename) {
        Role role = roles.get(rolename);
        if (role == null) {
            logger.warning(String.format("Unknown role: %s", rolename));
            return false;
        }

        return role.isAssumedByUser(user);
    }

    public List<Role> getRoles() {
        LinkedList<Role> rs = new LinkedList<>(roles.values());
        return rs;
    }

    public void addRule(String roleName, String roleRuleType, String value) {
        Role role = roles.get(roleName);
        if (role == null) {
            logger.warning(String.format("Role %s doesn't exist", roleName));
        }
        else {
            try {
                role.addRule(roleRuleType, value);
            }
            catch (RuleAlreadyExists ex) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", ex.getMessage()));
                logger.warning(ex.getMessage());
            }
        }
    }

    public void addRule(String rolename, RoleRule rule) {
        Role role = roles.get(rolename);
        if (role == null) {
            logger.severe(String.format("Cannot add rule to non existing role %s",
                    rolename));
            return;
        }
        try {
            role.addRule(rule);
        }
        catch (RuleAlreadyExists ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", ex.getMessage()));
            logger.warning(ex.getMessage());
        }
    }

    public void removeRuleFromRole(String roleName, String ruleName, String value) {
        Role role = roles.get(roleName);

        if (role == null) {
            logger.severe(String.format("Cannot remove rule from non existing role %s",
                    roleName));
            return;
        }
        role.removeRule(ruleName, value);
    }

    public void removeRuleFromRole(Role role, RoleRule rule) {
        role.removeRule(rule);
        removeRuleFromDatabase(role, rule);
    }

    private void removeRuleFromDatabase(Role role, RoleRule rule) {
        try {
            Connection con = getDatabaseConnection();
            Statement stm = con.createStatement();
            String sql = String.format("DELETE FROM role_rules WHERE role_id = '%s' AND roleRuleName = '%s' AND param = '%s';",
                    role.getId(), rule.getRoleType(), rule.getValue());
            logger.info(sql);
            stm.executeUpdate(sql);
        }
        catch (ClassNotFoundException | SQLException ex) {
            logger.warning(String.format("Cannot remove rule from role: %s", ex.getMessage()));
        }
    }
}
