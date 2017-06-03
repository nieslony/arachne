/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.utils.classfinder.BeanInjector;
import at.nieslony.utils.classfinder.ClassFinder;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

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

    @PostConstruct
    public void init() {
        logger.info("Initializing RoleRuleFactoryCollection");
        ClassFinder classFinder = new ClassFinder((getClass().getClassLoader()));

        List<Class> factories = null;
        try {
            factories = classFinder.getAllClassesImplementing(RoleRuleFactory.class);
        }
        catch (ClassNotFoundException | IOException | URISyntaxException ex) {
            logger.warning(String.format("Cannot load classes: %s", ex.getMessage()));
        }

        FacesContext ctx = FacesContext.getCurrentInstance();
        for (Class c : factories) {
            try {
                RoleRuleFactory roleRuleFactory = (RoleRuleFactory) c.newInstance();
                addRoleRuleFactory(roleRuleFactory);

                BeanInjector.injectStaticBeans(ctx, c);
            }
            catch (IllegalAccessException | InstantiationException ex) {
                logger.warning(String.format("Cannot create role rule factory %s: %s",
                        c.getName(), ex.getMessage()));
            }
            catch (NoSuchMethodException ex) {
                logger.warning(String.format("Cannot find method in class %s: %s",
                    c.getName(), ex.getMessage()));
            }
            catch (IllegalArgumentException | InvocationTargetException ex) {
                logger.warning(String.format("Cannot invoke method: %s",
                        c.getName(),  ex.getMessage()));
            }
        }
    }

    public void addRoleRuleFactory(RoleRuleFactory factory) {
        logger.info(String.format("Add RoleRuleFactory %s", factory.getRoleRuleName()));
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
