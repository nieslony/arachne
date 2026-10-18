/*
 * Copyright (C) 2026 claas
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
package at.nieslony.arachne.webserver;

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
public class ReverseProxySettings extends AbstractSettingsGroup {

    public enum AddAuthentication {
        NONE("None"),
        KERBEROS("Kerberos");

        private final String label;

        AddAuthentication(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private String proxyPath = "/arachne";

    private AddAuthentication addAuthentication = AddAuthentication.NONE;
    private String keytabFile = "/etc/httpd/krb5.keytab";
}
