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
