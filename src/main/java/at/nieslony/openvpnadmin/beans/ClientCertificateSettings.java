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

package at.nieslony.openvpnadmin.beans;

import at.nieslony.databasepropertiesstorage.PropertyGroup;
import at.nieslony.openvpnadmin.beans.base.ClientCertificateSettingsBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.logging.Logger;

@ApplicationScoped
@Named
public class ClientCertificateSettings
    extends ClientCertificateSettingsBase
    implements Serializable
{
    public ClientCertificateSettings() {
    }

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    private PropertiesStorageBean propertiesStorage;

    public void setPropertiesStorage(PropertiesStorageBean ps) {
        propertiesStorage = ps;
    }

    @Override
    protected PropertyGroup getPropertyGroup() {
        PropertyGroup  pg = null;

        try {
            return propertiesStorage.getGroup("client-certificate-settings", true);
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot get property group client-certificate-settings: %s",
                ex.getMessage()));
            if (ex.getNextException() != null)
            logger.severe(String.format("Cannot get property group client-certificate-settings: %s",
                ex.getNextException().getMessage()));
        }

        return null;
    }

    public String getKeyAlgorithm() {
        String sa = getSignatureAlgorithm();
        String[] saSplit = sa.split("with");
        if (saSplit.length != 2) {
            return "???";
        }
        else {
            return saSplit[1];
        }
    }

}
