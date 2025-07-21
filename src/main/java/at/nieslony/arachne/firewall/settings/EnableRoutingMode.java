/*
 * Copyright (C) 2025 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.firewall.settings;

/**
 *
 * @author claas
 */
public enum EnableRoutingMode {
    OFF("Don't change"),
    ENABLE("Enable on Startup"),
    RESTORE_ON_EXIT("Enable and Restore Status on Exit");

    private final String label;

    private EnableRoutingMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

}
