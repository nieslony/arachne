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

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.LocalUser;
import at.nieslony.openvpnadmin.UserFactory;
import at.nieslony.utils.DbUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
@ApplicationScoped
@Named
public class LocalUserFactory
        extends UserFactory
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private final String USERS_TABLE = "users";

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

    @Override
    public AbstractUser addUser(String username) {
        LocalUser user = null;

        try {
            Connection con = databaseSettings.getDatabaseConnection();

            user = new LocalUser(this, username);
            Statement stm = con.createStatement();
            String sql = String.format("INSERT INTO %s (username) VALUES('%s');",
                    USERS_TABLE, username);
            stm.executeUpdate(sql);
            stm.close();

            stm = con.createStatement();
            sql = String.format("SELECT id FROM %s WHERE username = '%s';",
                    USERS_TABLE, username);
            ResultSet result = stm.executeQuery(sql);
            if (result.next()) {
                user.setId(result.getString("id"));
            }
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot create local user %s: %s",
                    username, ex.getMessage()));
            ex = ex.getNextException();
            if (ex != null)
                logger.severe(ex.getMessage());
        }
        catch (ClassNotFoundException ex) {
            logger.severe(String.format("Cannot create local user %s: %s",
                    username, ex.getMessage()));
        }

        return user;
    }

    public Connection getDatabaseConnection()
            throws ClassNotFoundException, SQLException
    {
        return databaseSettings.getDatabaseConnection();
    }

    @Override
    public AbstractUser findUser(String username) {
        LocalUser user = null;

        try {
            Connection con = databaseSettings.getDatabaseConnection();
            Statement stm = con.createStatement();

            String sql = String.format("SELECT * FROM %s WHERE username = '%s';",
                    USERS_TABLE, username);
            ResultSet result = stm.executeQuery(sql);
            if (result.next()) {
                user = new LocalUser(this, username);
                user.setId(result.getString("id"));
                user.setEmail(result.getString("email"));
                user.setFullName(result.getString("fullName"));
                user.setGivenName(result.getString("givenName"));
                user.setSurName(result.getString("surName"));
                user.setPasswordHash(result.getString("password"));
            }
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot find local user %s: %s",
                    username, ex.getMessage()));
            ex = ex.getNextException();
            if (ex != null)
                logger.severe(ex.getMessage());
        }
        catch (ClassNotFoundException ex) {
            logger.severe(String.format("Cannot find local user %s: %s",
                    username, ex.getMessage()));
        }

        return user;
    }

    @Override
    public boolean removeUser(String username)
            throws ClassNotFoundException, SQLException
    {
        Connection con = databaseSettings.getDatabaseConnection();
        Statement stm = con.createStatement();
        String sql = String.format("DELETE FROM %s WHERE username = '%s';",
                USERS_TABLE, username);
        int ret = stm.executeUpdate(sql);
        return ret == 1;
    }

    @Override
    public List<AbstractUser> getAllUsers()
            throws ClassNotFoundException, SQLException
    {
        List<AbstractUser> users = new LinkedList<>();

        Connection con = databaseSettings.getDatabaseConnection();
        Statement stm = con.createStatement();
        String sql = String.format("SELECT * FROM %s", USERS_TABLE);
        logger.info(String.format("Exec uting: %s", sql));
        ResultSet result = stm.executeQuery(sql);
        while (result.next()) {
            LocalUser user = new LocalUser(this, result);
            users.add(user);
        }

        return users;
    }

   public void createTables()
            throws IOException, SQLException, ClassNotFoundException
    {
        logger.info("Creating tables for propertiesStorage...");
        String resourceName = "/WEB-INF/sql/create-local-users-and-roles.sql";
        Reader r = null;
        try {
            ExternalContext ectx = FacesContext.getCurrentInstance().getExternalContext();
            InputStream is = ectx.getResourceAsStream(resourceName);
            r = new InputStreamReader(is);

            Connection con = databaseSettings.getDatabaseConnection();
            if (con == null) {
                logger.severe("Cannot get database connection");
            }
            DbUtils.executeSql(con, r);
        }
        finally {
            if (r != null) {
                try {
                    r.close();
                }
                catch (IOException ex) {
                    logger.severe(String.format("Cannot close reader: %s", ex.getMessage()));
                }
            }
        }
    }
}
