/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.RoleRuleIsUserFactory;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
public class RoleRuleIsUser
        extends RoleRule
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    /*
    public RoleRuleIsUser() {
    }
*/
    public RoleRuleIsUser(RoleRuleIsUserFactory factory, String username) {
        super(factory, username);
    }

    @Override
    public boolean isAssumedByUser(AbstractUser user) {
        if (user == null) {
            logger.warning("null user supplied");
            return false;
        }
        else {
            String value = getValue();
            if (value != null) {
                    logger.info("RoleRuleIsUser has no value");
                    return getValue().equals(user.getUsername());
            }
            return false;
        }
    }
}
