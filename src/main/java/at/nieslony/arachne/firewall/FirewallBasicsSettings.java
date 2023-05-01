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

import at.nieslony.arachne.settings.Settings;
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
public class FirewallBasicsSettings {

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

    private final static String SK_FIREWALL_ENABLE = "firewall.enable";
    private final static String SK_FIREFALL_ZONE = "firewall.zone";
    private final static String SK_FIREWALL_ROUTINGMODE = "firewall.eńable-routing-mode";

    private boolean enableFirewall;
    private String firewallZone;
    private EnableRoutingMode enableRoutreMode;

    public FirewallBasicsSettings(Settings settings) {
        enableFirewall = settings.getBoolean(SK_FIREWALL_ENABLE, false);
        firewallZone = settings.get(SK_FIREFALL_ZONE, "arachne");
        enableRoutreMode = EnableRoutingMode.valueOf(
                settings.get(
                        SK_FIREWALL_ROUTINGMODE,
                        EnableRoutingMode.RESTORE_ON_EXIT.name()
                )
        );
    }

    public void save(Settings settings) {
        settings.put(SK_FIREWALL_ENABLE, enableFirewall);
        settings.put(SK_FIREFALL_ZONE, firewallZone);
        settings.put(SK_FIREWALL_ROUTINGMODE, enableRoutreMode.name());
    }
}
