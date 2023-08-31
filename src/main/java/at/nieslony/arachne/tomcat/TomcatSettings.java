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
package at.nieslony.arachne.tomcat;

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import java.security.SecureRandom;
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
public class TomcatSettings extends AbstractSettingsGroup {

    private boolean enableAjpConnector = false;
    private int ajpPort = 8009;
    private boolean enableAjpSecret = true;
    private String ajpSecret = createSecret();
    private String ajpLocation = "/arachne";

    public TomcatSettings() {
    }

    public String createSecret() {
        SecureRandom random = new SecureRandom();

        String password = random.ints(32, 127)
                .filter(c -> Character.isLetterOrDigit(c))
                .limit(24)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return password;
    }
}
