/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.Roles;
import at.nieslony.openvpnadmin.errorhandling.RuleAlreadyExists;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
public class Role
        implements Serializable
{
    private String name = "noname";
    private final List<RoleRule> rules = new LinkedList<>();
    private static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private String id;
    private Roles roles;

    public String getName() {
        return name;
    }

    public Role() {
    }

    public void init(Roles roles, String id, String name)
            throws SQLException, ClassNotFoundException
    {
        this.id = id;
        this.roles = roles;
        this.name = name;

        logger.info(String.format("Initializing role %s", name));

        Connection con = roles.getDatabaseConnection();
        Statement stm = con.createStatement();
        String sql = String.format("SELECT * FROM role_rules WHERE role_id = '%s';",
                id);
        logger.info(String.format("Executing sql: %s", sql));
        ResultSet result = stm.executeQuery(sql);
        while (result.next()) {
            String roleRuleName = result.getString("roleRuleName");
            String param = result.getString("param");

            logger.info(String.format("Found rule %s(%s)", roleRuleName, param));

            RoleRule rule = roles.getRoleRuleFactoryCollection().createRoleRule(roleRuleName, param);
            rules.add(rule);
        }
    }

    public String getId() {
        return id;
    }

    public boolean isAssumedByUser(AbstractUser user) {
        if (user == null) {
            logger.warning("No user supplied => no rule");
            return false;
        }

        for (RoleRule r : rules) {
            if (r == null) {
                logger.severe(String.format("Found null rule in role %s", getName()));
                continue;
            }
            boolean hasRole = r.isAssumedByUser(user);
            logger.info(String.format("role rule check (%s=%s) for user %s: %b",
                    r.getRoleType(), r.getValue(), user, hasRole));
            if (hasRole)
                return true;
        }
        logger.info(String.format("No matching rule found for user %s => user doesn't have role %s",
                user, name));
        return false;
    }

    public List<RoleRule> getRules() {
        return rules;
    }

    public void addRule(String roleRuleType, String param)
            throws RuleAlreadyExists
    {
        RoleRule rule = roles.getRoleRuleFactoryCollection().createRoleRule(roleRuleType, param);
        addRule(rule);
    }

    public void addRule(RoleRule rule)
            throws RuleAlreadyExists
    {
        for (RoleRule r: rules)
        {
            if (r.getValue().equals(rule.getValue()) &
                    r.getRoleType().equals(rule.getRoleType())
                    ) {
                throw new RuleAlreadyExists(getName(), r.getRoleType(), r.getValue());
            }
        }

        rules.add(rule);

        try {
            Connection con = roles.getDatabaseConnection();
            Statement stm = con.createStatement();
            String sql = String.format("INSERT INTO role_rules VALUES(%s, '%s', '%s');",
                    id, rule.getRoleType(), rule.getValue());
            logger.info(String.format("Executing sql: %s", sql));
            stm.executeUpdate(sql);
        }
        catch (ClassNotFoundException | SQLException ex) {
            logger.warning(String.format("Cannot add rule: %s", ex.getMessage()));
        }
    }

    public void removeRule(String ruleName, String value) {
        for (RoleRule rule: getRules()) {
            if (rule.getRoleType().equals(ruleName) && rule.getValue().equals(value)) {
                removeRule(rule);
                break;
            }
        }
    }
    
    public void removeRule(RoleRule rule) {
        rules.remove(rule);
        
        
    }
}
