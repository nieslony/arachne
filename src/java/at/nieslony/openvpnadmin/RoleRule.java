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

import at.nieslony.openvpnadmin.beans.RoleRuleFactory;
import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
abstract public class RoleRule
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());
    private String value;
    private RoleRuleFactory factory;

    public RoleRule() {
    }

    public void init (RoleRuleFactory factory, String value) {
        this.value = value;
        this.factory = factory;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            logger.info("Same object => equal");
            return true;
        }
        if (obj == null) {
            logger.info("null object => not equal");
            return false;
        }
        if (getClass() != obj.getClass()) {
            logger.info("different classes => not equal");
            return false;
        }
        final RoleRule other = (RoleRule) obj;
        if (!Objects.equals(this.value, other.value)) {
            logger.info(String.format("different values (%s, %s)=> not equal", this.value, other.value));
            return false;
        }
        if (!Objects.equals(this.factory, other.factory)) {
            logger.info("different factories => not equal");
            return false;
        }
        logger.info("object equal");

        return true;
    }

    @Override
    public int hashCode() {
        return value.hashCode() * factory.hashCode();
    }

    abstract public boolean isAssumedByUser(AbstractUser user);
    abstract public String getValueLabel();

    public String getRoleType() {
        return factory.getRoleRuleName();
    }

    public String getRoleDescription() {
        return factory.getDescriptionString();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
