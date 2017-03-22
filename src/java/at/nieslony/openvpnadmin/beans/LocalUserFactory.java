/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.LocalUser;
import at.nieslony.openvpnadmin.User;
import at.nieslony.openvpnadmin.UserFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private final String USERS_TABLE = "users";

    @ManagedProperty(value = "#{databaseSettings}")
    private DatabaseSettings databaseSettings;

    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }

    public User addUser(String username) {
        LocalUser user = null;

        try {
            Connection con = databaseSettings.getDatabseConnection();

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
        return databaseSettings.getDatabseConnection();
    }

    @Override
    public User findUser(String username) {
        LocalUser user = null;

        try {
            Connection con = databaseSettings.getDatabseConnection();
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
}
