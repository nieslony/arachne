/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.errorhandling;

/**
 *
 * @author claas
 */
public class RuleAlreadyExists extends Exception {
    /**
     * Constructs an instance of <code>RuleAlreadyExists</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public RuleAlreadyExists(String role, String ruleType, String ruleValue) {
        super(String.format("Rule %s=%s for role %s already exists",
                ruleType, ruleValue, role));
    }
}
