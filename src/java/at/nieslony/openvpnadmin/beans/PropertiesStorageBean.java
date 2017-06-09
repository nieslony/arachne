/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.utils.DbUtils;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

/**
 *
 * @author claas
 */
@ManagedBean(name="propertiesStorage")
@ApplicationScoped
public class PropertiesStorageBean
        extends at.nieslony.databasepropertiesstorage.PropertiesStorage
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

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

    /**
     * Creates a new instance of PropertiesStorage
     */
    public PropertiesStorageBean() {
    }

    @PostConstruct
    public void init() {
        setCacheTimeout(1000L * 60 * 10);
        try {
            setConnection(databaseSettings.getDatabseConnection());
        }
        catch (ClassNotFoundException | SQLException ex) {
            logger.severe(String.format("Cannot set database connection: %s",
                    ex.getMessage()));
        }
        setPropsTable("properties");
        setPropGroupsTable("propertyGroups");
    }

    @Override
    public Connection getConnection() {
        try {
            return databaseSettings.getDatabseConnection();
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public void createTables()
            throws ClassNotFoundException, IOException, SQLException
    {
        logger.info("Creating tables for propertiesStorage...");
        String resourceName = "create-properties-storage.sql";
        Reader r = null;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
            if (is != null)
                r = new InputStreamReader(is);
            else {
                r = new FileReader(String.format("%s/%s", folderFactory.getSqlDir(), resourceName));
            }

            if (r == null) {
                logger.severe(String.format("Cannot open %s as resource", resourceName));
            }
            Connection con = databaseSettings.getDatabseConnection();
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
