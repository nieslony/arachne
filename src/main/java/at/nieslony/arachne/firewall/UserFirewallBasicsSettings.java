/*
 * Copyright (C) 2023 claas
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
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class UserFirewallBasicsSettings extends AbstractSettingsGroup {

    public enum EnableRoutingMode {
        OFF("Don't change"),
        ENABLE("Enable on Startup"),
        RESTORE_ON_EXIT("Enable and Restore Status on Exit");

        final private String label;

        private EnableRoutingMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum IcmpRules {
        ALLOW_ALL("Allow all Pings"),
        ALLOW_ALL_GRANTED("Allow Pings to all granted hosts"),
        DENY("Deny All");

        final private String label;

        private IcmpRules(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private boolean enableFirewall = false;
    private String firewallZone = "arachne-user";
    private EnableRoutingMode enableRoutingMode = EnableRoutingMode.ENABLE;
    private IcmpRules icmpRules = IcmpRules.ALLOW_ALL_GRANTED;
}
