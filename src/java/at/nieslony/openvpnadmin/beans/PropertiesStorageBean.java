/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.utils.DbUtils;
import java.io.IOException;
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

    /**
     * Creates a new instance of PropertiesStorage
     */
    public PropertiesStorageBean() {
    }

    @PostConstruct
    public void init() {
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
    {
        logger.info("Creating tables for propertiesStorage...");
        String resourceName = "create-tables.sql";
        Reader r = null;
        try {
            r = new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream(resourceName)
            );
            if (r == null) {
                logger.severe(String.format("Cannot open %s as resource", resourceName));
            }
            Connection con = databaseSettings.getDatabseConnection();
            if (con == null) {
                logger.severe("Cannot get database connection");
            }
            DbUtils.executeSql(con, r);

            /*            Statement stm = con.createStatement();
            stm.addBatch("CREATE TABLE propertyGroups (    id SERIAL NOT NULL PRIMARY KEY, name text NOT NULL UNIQUE );");
            stm.addBatch("CREATE TABLE properties (    group_id SERIAL references propertyGroups(id),    name varchar NOT NULL UNIQUE,    value varchar,    UNIQUE (group_id, name));");
            stm.addBatch("CREATE OR REPLACE FUNCTION set_property(_group_id int, _name varchar, _value varchar) RETURNS VOID AS $$     DECLARE     BEGIN         UPDATE properties SET value = _value WHERE group_id = _group_id AND name = _name;        IF NOT FOUND THEN             INSERT INTO properties values(_group_id, _name, _value);         END IF;     END;     $$ LANGUAGE 'plpgsql';");
            stm.executeBatch();
            stm.close();
*/

            //con.commit();
        }
        catch (IOException ex) {
            logger.severe(String.format("Cannot read sql: %s", ex.getMessage()));
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot execute sql: %s", ex.getMessage()));
            SQLException ne = ex.getNextException();
            logger.severe(ne.getMessage());
        }
        catch (ClassNotFoundException ex) {
            logger.severe(String.format("Class nor found: %s", ex.toString()));
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
