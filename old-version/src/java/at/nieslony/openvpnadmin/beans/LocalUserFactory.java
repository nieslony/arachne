/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.LocalUser;
import at.nieslony.openvpnadmin.UserFactory;
import at.nieslony.utils.DbUtils;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class LocalUserFactory
        extends UserFactory
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private final String USERS_TABLE = "users";

    @ManagedProperty(value = "#{databaseSettings}")
    private DatabaseSettings databaseSettings;

    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }

    @ManagedProperty(value = "#{folderFactory}")
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
        String resourceName = "create-local-users-and-roles.sql";
        Reader r = null;
        try {
            r = new FileReader(String.format("%s/%s", folderFactory.getSqlDir(), resourceName));

            if (r == null) {
                logger.severe(String.format("Cannot open %s as resource", resourceName));
            }
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
