/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
public class Role implements Serializable {
    private String name = "noname";
    private final List<RoleRule> rules = new LinkedList<>();
    private static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    public String getName() {
        return name;
    }

    public Role() {

    }

    public Role(String name) {
        this.name = name;
    }

    public boolean isAssumedByUser(String user) {
        for (RoleRule r : rules) {
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

    public void load(Reader re) throws IOException {
        BufferedReader br = new BufferedReader(re);
        logger.info(String.format("Loading role %s", name));

        name = br.readLine();
        rules.clear();
        for (String line; (line = br.readLine()) != null; ) {
            String[] tokens = line.split(":");
            if (tokens.length != 2) {
                logger.warning(String.format("Systax error in line %s", line));
                continue;
            }

            String ruleClass = tokens[0];
            String ruleValue = tokens[1];

            try {
                RoleRule rule = (RoleRule) Class.forName(ruleClass).newInstance();
                rule.setValue(ruleValue);
                rules.add(rule);
            }
            catch (ClassNotFoundException ex) {
                logger.warning(String.format("Rule class not found: %s", ruleClass));
            }
            catch (InstantiationException | IllegalAccessException ex) {
                logger.severe(ex.getMessage());
            }
        }
    }

    public void save(Writer wr) {
        PrintWriter pr = new PrintWriter(wr);

        pr.println(name);
        for (RoleRule r : rules) {
            pr.println(r.getClass().getName() + ":" + r.getValue());
        }

        pr.flush();
    }

    public List<RoleRule> getRules() {
        return rules;
    }

    public void addRule(RoleRule rule) {
        rules.add(rule);
    }

    public void removeRole(RoleRule rule) {
        rules.remove(rule);
    }
}
