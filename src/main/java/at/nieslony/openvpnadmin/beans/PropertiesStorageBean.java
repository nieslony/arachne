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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.postgresql.util.PSQLException;

/**
 *
 * @author claas
 */
@ApplicationScoped
@Named("propertiesStorage")
public class PropertiesStorageBean
        extends at.nieslony.databasepropertiesstorage.PropertiesStorage
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    private DatabaseSettings databaseSettings;

    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }

    @Inject
    private FolderFactory folderFactory;

    public void setFolderFactory(FolderFactory ff) {
        folderFactory = ff;
    }

    /**
     * Creates a new instance of PropertiesStorage
     */
    public PropertiesStorageBean() {
    }

    @PostConstruct
    public void init() {
        setCacheTimeout(1000L * 60 * 10);
        try {
            setConnection(databaseSettings.getDatabaseConnection());
        }
        catch (ClassNotFoundException | SQLException ex) {
            logger.severe(String.format("Cannot set database connection: %s",
                    ex.getMessage()));
        }
        setPropsTable("properties");
        setPropGroupsTable("propertyGroups");
    }

    @Override
    public Connection getConnection()
            throws SQLException
    {
        try {
            return databaseSettings.getDatabaseConnection();
        }
        catch (PSQLException ex) {
            throw ex;
        }
        catch (ClassNotFoundException | SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
