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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.postgresql.util.PSQLException;

/**
 *
 * @author claas
 */
@ApplicationScoped
@Named
public class DatabaseSettings
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private static final String PROP_HOST = "host";
    private static final String PROP_PORT = "port";
    private static final String PROP_DB_NAME = "database-name";
    private static final String PROP_DB_USER = "database-user";
    private static final String PROP_DB_PASSWORD = "database-password";

    private static final String DB_SETTINGS_FILE = "/database.properties";

    private String host;
    private int port;
    private String databaseName;
    private String databaseUser;
    private String databasePassword;

    private boolean valid = false;

    private transient Connection con = null;

    @Inject
    private FolderFactory folderFactory;

    public void setFolderFactory(FolderFactory ff) {
        folderFactory = ff;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public void setDatabaseUser(String databaseUser) {
        this.databaseUser = databaseUser;
    }


    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Creates a new instance of DatrabaseSettings
     */
    public DatabaseSettings() {
    }

    public void load()
            throws IOException
    {
        Properties props = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(getPropsFileName());
            props.load(fis);
            logger.info("database properties successfully loaded, setting status to VALID.");
            valid = true;
        }
        finally {
            if (fis != null)
                fis.close();
        }

        host = props.getProperty(PROP_HOST, "localhost");
        port = 5432;
        // port = Integer.getInteger(props.getProperty(PROP_PORT, "5432"));
        databaseName = props.getProperty(PROP_DB_NAME, "openvpnadmin");
        databaseUser = props.getProperty(PROP_DB_USER, "openvpnadmin");
        databasePassword =
                new String (
                        Base64.getDecoder().decode(
                            props.getProperty(PROP_DB_PASSWORD, "")
                ));
    }

    private String getPropsFileName() {
        String fn = String.format("%s/%s",
                folderFactory.getConfigDir(),
                DB_SETTINGS_FILE);
        return fn;
    }

    public void save()
            throws FileNotFoundException, IOException
    {
        Properties props = new Properties();
        props.setProperty(PROP_HOST, host);
        props.setProperty(PROP_PORT, String.valueOf(port));
        props.setProperty(PROP_DB_NAME, databaseName);
        props.setProperty(PROP_DB_USER, databaseUser);
        props.setProperty(PROP_DB_PASSWORD,
                Base64.getEncoder().encodeToString(databasePassword.getBytes()));

        logger.info(String.format("Saving database settings to %s", getPropsFileName()));
        FileOutputStream fos = null;
        try {
            File outputDir = new File(folderFactory.getConfigDir());
            outputDir.mkdirs();

            fos = new FileOutputStream(getPropsFileName());
            props.store(fos, "");
            fos.close();
            logger.info("database properties successfully saved, setting status to VALID.");
            valid = true;
        }
        finally {
            if (fos != null)
                fos.close();
        }
    }

    public Connection getDatabaseConnection()
            throws PSQLException, ClassNotFoundException, SQLException
    {
        if (con == null) {
            try {
                load();
            }
            catch (IOException ex) {
                logger.warning(
                        String.format("Cannot load database settings: %s", ex.getMessage()));
            }

            Class.forName("org.postgresql.Driver");
            String conUrl = String.format("jdbc:postgresql://%s:%d/%s",
                    host,
                    port,
                    databaseName);
            con = DriverManager.getConnection(conUrl, databaseUser, databasePassword);
            con.setAutoCommit(true);
        }

        return con;
    }

    public void closeDatabaseConnection()
            throws SQLException
    {
        if (con != null) {
            con.close();
            con = null;
        }
    }

    public boolean isValid() {
        if (valid)
            logger.info("databaseSettings: valid");
        else
            logger.info("databaseSettings: not valid");
        return valid;
    }

    public void destroy() {
        if (con != null) {
            try {
                con.close();
            }
            catch (SQLException ex) {
                logger.warning(String.format("Cannot close database connection: %s",
                        ex.getMessage()));
            }
        }
    }
}
