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
package at.nieslony.arachne.kerberos;

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.utils.net.NetUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class KerberosSettings extends AbstractSettingsGroup {

    private boolean enableKrbAuth = false;
    private String keytabPath = "";
    private String servicePrincipal = "HTTP/" + NetUtils.myHostname();

    public void setDefaultKeytabPath() {
        keytabPath = FolderFactory.getInstance().getDefaultKeytabPath();
    }
}
