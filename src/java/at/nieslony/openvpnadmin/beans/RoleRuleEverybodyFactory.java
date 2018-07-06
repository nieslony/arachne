/*
 * Copyright (C) 2018 claas
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

import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.openvpnadmin.RoleRuleEverybody;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author claas
 */
public class RoleRuleEverybodyFactory
        implements RoleRuleFactory, Serializable
{

    @Override
    public RoleRule createRule(String username) {
        RoleRuleEverybody rule = new RoleRuleEverybody();
        rule.init(this, "");

        return rule;
    }

    @Override
    public String getRoleRuleName() {
        return "everybody";
    }

    @Override
    public String getDescriptionString() {
        return  "Everybody";
    }

    @Override
    public List<String> completeValue(String userPattern) {
        final List<String> emptyList = new LinkedList<>();
        return emptyList;
    }
}
