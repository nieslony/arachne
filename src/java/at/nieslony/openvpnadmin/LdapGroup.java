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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
public class LdapGroup {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private String name;
    private String description;
    private final List<String> memberUids = new LinkedList<>();
    private final List<String> memberDNs = new LinkedList<>();

    public boolean hasMember(LdapUser user) {
        for (String m: memberUids) {
            if (m.equals(user.getUsername()))
                return true;
            logger.info(String.format("%s != %s", m, user.getUsername()));
        }
        for (String m: memberDNs) {
            if (m.equals(user.getDn()))
                return true;
            logger.info(String.format("%s != %s", m, user.getDn()));
        }
        return false;
    }

    public List<String> getMemberDNs() {
        return memberDNs;
    }

    public List<String> getMemberUids() {
        return memberUids;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
