/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.Role;
import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.openvpnadmin.errorhandling.RuleAlreadyExists;
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
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class Roles implements Serializable {
    private Map<String, Role> roles = new HashMap<>();
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{databaseSettings}")
    private DatabaseSettings databaseSettings;

    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }

    @ManagedProperty(value = "#{roleRuleFactoryCollection}")
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

    public void removeRuleFromRole(Role role, RoleRule rule) {
        role.removeRole(rule);
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
