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
package at.nieslony.arachne;

import at.nieslony.arachne.settings.Settings;
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
public class TomcatSettings {

    private static final String SK_TOMCAT_ENABLE_AJP = "tomcat.enable-ajp";
    private static final String SK_TOMCAT_AJP_PORT = "tomcat.ajp-port";
    private static final String SK_TOMCAT_ENABLE_AJP_SECRET = "tomcat.enable-ajp-secret";
    private static final String SK_TOMCAT_AJP_SECRET = "tomcat.ajp-secret";
    private static final String SK_TOMCAT_AJP_LOCATION = "tomcat.ajp-location";

    private boolean enableAjpConnector;
    private int ajpPort;
    private boolean enableAjpSecret;
    private String ajpSecret;
    private String ajpLocation;

    public TomcatSettings() {
    }

    public TomcatSettings(Settings settings) {
        enableAjpConnector = settings.getBoolean(SK_TOMCAT_ENABLE_AJP, false);
        ajpPort = settings.getInt(SK_TOMCAT_AJP_PORT, 8009);
        enableAjpSecret = settings.getBoolean(SK_TOMCAT_ENABLE_AJP_SECRET, true);
        ajpSecret = settings.get(SK_TOMCAT_AJP_SECRET, createSecret());
        ajpLocation = settings.get(SK_TOMCAT_AJP_LOCATION, "/arachne");
    }

    public void save(Settings settings) {
        settings.put(SK_TOMCAT_ENABLE_AJP, enableAjpConnector);
        settings.put(SK_TOMCAT_AJP_PORT, ajpPort);
        settings.put(SK_TOMCAT_ENABLE_AJP_SECRET, enableAjpSecret);
        settings.put(SK_TOMCAT_AJP_SECRET, ajpSecret);
        settings.put(SK_TOMCAT_AJP_LOCATION, ajpLocation);
    }

    public String createSecret() {
        SecureRandom random = new SecureRandom();

        String password = random.ints(32, 127)
                .filter(c -> Character.isLetterOrDigit(c))
                .limit(16)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return password;
    }
}
