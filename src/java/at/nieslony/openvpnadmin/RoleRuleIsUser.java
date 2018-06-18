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

    public RoleRuleIsUser() {
    }

    public void init (RoleRuleIsUserFactory factory, String username) {
        super.init(factory, username);
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
