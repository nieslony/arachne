/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.RoleRule;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class RoleRuleFactoryCollection
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    HashMap<String, RoleRuleFactory> factories = new HashMap<>();

    /**
     * Creates a new instance of RoleRuleFactoryCollection
     */
    public RoleRuleFactoryCollection() {
    }

    public void addRoleRuleFactory(RoleRuleFactory factory) {
        factories.put(factory.getRoleRuleName(), factory);
    }

    public RoleRule createRoleRule(String ruleName, String value) {
        RoleRuleFactory factory = factories.get(ruleName);
        RoleRule rule = null;

        if (factory == null) {
            logger.warning(String.format("No such role rule factory: %s", ruleName));
        }
        else {
            rule = factory.createRule(value);
        }

        return rule;
    }

    public Set<String> getRoleRuleNames() {
        return factories.keySet();
    }

    public Collection<RoleRuleFactory> getFactories() {
        return factories.values();
    }

    public RoleRuleFactory getFactory(String ruleType) {
        return factories.get(ruleType);
    }
}
