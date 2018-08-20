/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.beans.firewallzone.Entry;
import at.nieslony.utils.DbUtils;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
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
public class FirewallSettings implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    List<Entry> incomingEntries;
    List<Entry> outgoingEntries;

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;
    public void setFolderFactory(FolderFactory ff) {
        this.folderFactory = ff;
    }

    @ManagedProperty(value = "#{databaseSettings}")
    private DatabaseSettings databaseSettings;
    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }

    public FirewallSettings() {
        incomingEntries = new LinkedList<>();
        outgoingEntries = new LinkedList<>();
    }

    public void createTables()
                throws IOException, SQLException, ClassNotFoundException
    {
        logger.info("Creating tables for propertiesStorage...");
        String resourceName = "create-firewall.sql";
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

    public List<Entry> getIncomingEntries() {
        return incomingEntries;
    }

    public List<Entry> getOutgoingEntries() {
        return outgoingEntries;
    }

    public void addIncomingEntry(Entry entry) {
        incomingEntries.add(entry);
    }
}
