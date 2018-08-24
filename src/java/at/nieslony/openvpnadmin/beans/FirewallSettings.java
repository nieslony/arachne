/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.openvpnadmin.beans.firewallzone.Entry;
import at.nieslony.openvpnadmin.beans.firewallzone.What;
import at.nieslony.openvpnadmin.beans.firewallzone.Where;
import at.nieslony.openvpnadmin.beans.firewallzone.Who;
import at.nieslony.utils.DbUtils;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
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

    static final String INSERT_ENTRY =
            "INSERT INTO firewallEntries (label, description, isActive) " +
            "VALUES (?, ?, ?) " +
            "RETURNING id;";
    static final String INSERT_WHO =
            "INSERT INTO firewallEntryWho (firewallEntry_id, ruleType, param) " +
            "VALUES (?, ?, ?);";
    static final String INSERT_WHERE_EVERYWHERE =
            "INSERT INTO firewallEntryWhere " +
            "(firewallEntry_id, whereType)" +
            "VALUES (?, ?);";
    static final String INSERT_WHERE_HOSTNAME =
            "INSERT INTO firewallEntryWhere " +
            "(firewallEntry_id, whereType, hostname)" +
            "VALUES (?, ?, ?);";
    static final String INSERT_WHERE_NETWORK =
            "INSERT INTO firewallEntryWhere " +
            "(firewallEntry_id, whereType, network)" +
            "VALUES (?, ?, ?::cidr);";
    static final String INSERT_WHAT_EVERYTHING =
            "INSERT INTO firewallEntryWhat (firewallEntry_id, WhatType)" +
            "VALUES (?, ?);";
    static final String INSERT_WHAT_PORT_LIST_PROTOCOL =
            "INSERT INTO firewallEntryWhat (firewallEntry_id, WhatType, ports, protocol)" +
            "VALUES (?, ?, ?, ?::protocol);";
    static final String INSERT_WHAT_PORT_PROTOCOL =
            "INSERT INTO firewallEntryWhat (firewallEntry_id, WhatType, port, protocol)" +
            "VALUES (?, ?, ?, ?::protocol);";
    static final String INSERT_WHAT_PORT_RAMGE_PROTOCOL =
            "INSERT INTO firewallEntryWhat (firewallEntry_id, WhatType, portFrom, portTo, protocol)" +
            "VALUES (?, ?, ?, ?, ?::protocol);";
    static final String INSERT_WHAT_SERVICE =
            "INSERT INTO firewallEntryWhat (firewallEntry_id, WhatType, service)" +
            "VALUES (?, ?, ?);";

    static final String GET_ALL_ENTRIES =
            "SELECT id, label, description, isActive FROM firewallEntries;";
    static final String GET_WHOS_FOR_ENTRY =
            "SELECT ruleType, param FROM firewallEntryWho " +
            "WHERE firewallEntry_id = ?;";
    static final String GET_WHERES_FOR_ENTRY =
            "SELECT whereType, hostname, network FROM firewallEntryWhere " +
            "WHERE firewallEntry_id = ?;";
    static final String GET_WHATS_FOR_ENTRY =
            "SELECT service, whatType, ports, port, portFrom, portTo, protocol " +
            "FROM firewallEntryWhat " +
            "WHERE firewallEntry_id = ?;";

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

    @ManagedProperty(value = "#{roleRuleFactoryCollection}")
    RoleRuleFactoryCollection roleRuleFactoryCollection;
    public void setRoleRuleFactoryCollection(RoleRuleFactoryCollection rrfc) {
        roleRuleFactoryCollection = rrfc;
    }

    @ManagedProperty(value = "#{firewallDServices}")
    FirewallDServices firewallDServices;
    public void setFirewallDServices(FirewallDServices fs) {
        firewallDServices = fs;
    }

    public FirewallSettings() {
        incomingEntries = new LinkedList<>();
        outgoingEntries = new LinkedList<>();
    }

    @PostConstruct
    public void init() {
        loadFromDb();
    }

    private void loadWhos(Entry entry)
            throws ClassNotFoundException, SQLException
    {
        Connection con = databaseSettings.getDatabaseConnection();
        PreparedStatement stm = con.prepareStatement(GET_WHOS_FOR_ENTRY);
        stm.setInt(1, entry.getId());
        ResultSet result = stm.executeQuery();
        while (result.next()) {
            String whoType = result.getString("ruleType");
            String whoValue = result.getString("param");
            RoleRule rr = roleRuleFactoryCollection.createRoleRule(whoType, whoValue);

            Who who = new Who();
            who.setTypeAndValue(rr);
            entry.getWhos().add(who);
        }
        result.close();
        stm.close();;
    }

    private void loadWheres(Entry entry)
            throws ClassNotFoundException, SQLException
    {
        Connection con = databaseSettings.getDatabaseConnection();
        PreparedStatement stm = con.prepareStatement(GET_WHERES_FOR_ENTRY);
        stm.setInt(1, entry.getId());
        ResultSet result = stm.executeQuery();
        while (result.next()) {
            Where where = new Where();

            String whereType = result.getString("whereType");
            String hostname = result.getString("hostname");
            String network = result.getString("network");

            where.setWhereType(Where.WhereType.valueOf(whereType));
            where.setHostname(hostname);
            if (network != null && !network.isEmpty()) {
                String[] components = network.split("/");
                where.setNetwork(components[0]);
                where.setMask(Integer.valueOf(components[1]));
            }

            entry.getWheres().add(where);
        }
        result.close();
        stm.close();
    }

    private void loadWhats(Entry entry)
            throws ClassNotFoundException, SQLException
    {
        Connection con = databaseSettings.getDatabaseConnection();
        PreparedStatement stm = con.prepareStatement(GET_WHATS_FOR_ENTRY);
        stm.setInt(1, entry.getId());
        ResultSet result = stm.executeQuery();
        while (result.next()) {
            String service = result.getString("service");
            String whatType = result.getString("whatType");
            int port = result.getInt("port");
            Array ports = result.getArray("ports");
            int portFrom = result.getInt("portFrom");
            int portTo = result.getInt("portTo");
            String protocol = result.getString("protocol");

            What what = new What();
            what.setWhatType(What.WhatType.valueOf(whatType));
            what.setPort(port);
            what.setPortFrom(portFrom);
            what.setPortTo(portTo);
            if (protocol != null)
                what.setProtocol(What.Protocol.valueOf(protocol));
            if (ports != null) {
                List<Integer> portsList = new LinkedList<>();
                Collections.addAll(portsList, (Integer[]) ports.getArray());
                what.setPorts(portsList);
            }
            if (service != null && !service.isEmpty()) {
                what.setService(firewallDServices.getServiceById(service));
            }
            entry.getWhats().add(what);
        }
        result.close();
        stm.close();
    }

    private void loadFromDb() {
        incomingEntries.clear();
        PreparedStatement stm = null;

        try {
            Connection con = databaseSettings.getDatabaseConnection();
            stm = con.prepareStatement(GET_ALL_ENTRIES);
            ResultSet result = stm.executeQuery();
            while (result.next()) {
                int pos = 1;
                int id = result.getInt("id");
                String label = result.getString("label");
                String description = result.getString("description");
                boolean isActive = result.getBoolean("isActive");
                Entry entry = new Entry(id, label, description, isActive);

                incomingEntries.add(entry);
                loadWhos(entry);
                loadWheres(entry);
                loadWhats(entry);
            }
            result.close();
        }
        catch (ClassNotFoundException | SQLException ex) {

        }
        finally {
            if (stm != null) {
                try {
                    stm.close();
                }
                catch (SQLException ex) {

                }
            }
        }
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

    private void addWhos(Entry entry)
            throws SQLException, ClassNotFoundException
    {
        logger.info("Adding whos");
        Connection con = databaseSettings.getDatabaseConnection();

        for (Who who : entry.getWhos()) {
            int posWhoStm = 1;
            try (PreparedStatement stm = con.prepareStatement(INSERT_WHO)) {
                stm.setInt(posWhoStm++, entry.getId());
                stm.setString(posWhoStm++, who.getWhoType());
                stm.setString(posWhoStm++, who.getWhoValue());
                stm.executeUpdate();
                stm.close();
            }
        }
    }

    private void addWheres(Entry entry)
            throws ClassNotFoundException, SQLException
    {
        logger.info("Adding wheres");
        Connection con = databaseSettings.getDatabaseConnection();

        for (Where where : entry.getWheres()) {
            int pos = 1;
            PreparedStatement stm = null;
            switch (where.getWhereType()) {
                case Everywhere:
                    stm = con.prepareStatement(INSERT_WHERE_EVERYWHERE);
                    stm.setInt(pos++, entry.getId());
                    stm.setString(pos++, where.getWhereType().getDescription());
                    break;
                case Hostname:
                    stm = con.prepareStatement(INSERT_WHERE_HOSTNAME);
                    stm.setInt(pos++, entry.getId());
                    stm.setString(pos++, where.getWhereType().getDescription());
                    stm.setString(pos++, where.getHostname());
                    break;
                case Network:
                    stm = con.prepareStatement(INSERT_WHERE_NETWORK);
                    stm.setInt(pos++, entry.getId());
                    stm.setString(pos++, where.getWhereType().getDescription());
                    stm.setString(pos++,
                            String.format("%s/%d",
                                    where.getNetwork(), where.getMask()
                            )
                    );
                    break;
            }
            if (stm != null) {
                stm.executeUpdate();
                stm.close();
            }
        }
    }

    private void addWhats(Entry entry)
            throws ClassNotFoundException, SQLException
    {
        logger.info("Adding whats");
        Connection con = databaseSettings.getDatabaseConnection();

        for (What what : entry.getWhats()) {
            int posWhatStm = 1;
            PreparedStatement stm = null;
            switch (what.getWhatType()) {
                case Everything:
                    stm = con.prepareStatement(INSERT_WHAT_EVERYTHING);
                    stm.setInt(posWhatStm++, entry.getId());
                    stm.setString(posWhatStm++, what.getWhatType().toString());
                    break;
                case PortListProtocol:
                    stm = con.prepareStatement(INSERT_WHAT_PORT_LIST_PROTOCOL);
                    stm.setInt(posWhatStm++, entry.getId());
                    stm.setString(posWhatStm++, what.getWhatType().toString());
                    stm.setArray(posWhatStm++,
                            con.createArrayOf("integer", what.getPorts().toArray()));
                    stm.setString(posWhatStm++, what.getProtocol().toString());
                    break;
                case PortProtocol:
                    stm = con.prepareStatement(INSERT_WHAT_PORT_PROTOCOL);
                    stm.setInt(posWhatStm++, entry.getId());
                    stm.setString(posWhatStm++, what.getWhatType().toString());
                    stm.setInt(posWhatStm++, what.getPort());
                    stm.setString(posWhatStm++, what.getProtocol().toString());
                    break;
                case PortRangeProtocol:
                    stm = con.prepareStatement(INSERT_WHAT_PORT_RAMGE_PROTOCOL);
                    stm.setInt(posWhatStm++, entry.getId());
                    stm.setString(posWhatStm++, what.getWhatType().toString());
                    stm.setInt(posWhatStm++, what.getPortFrom());
                    stm.setInt(posWhatStm++, what.getPortTo());
                    stm.setString(posWhatStm++, what.getProtocol().toString());
                    break;
                case Service:
                    stm = con.prepareStatement(INSERT_WHAT_SERVICE);
                    stm.setInt(posWhatStm++, entry.getId());
                    stm.setString(posWhatStm++, what.getWhatType().toString());
                    stm.setString(posWhatStm++, what.getService().getId());
                    break;
            }
            if (stm != null) {
                stm.execute();
                stm.close();
            }
        }
    }

    public void addIncomingEntry(Entry entry)
            throws ClassNotFoundException, SQLException
    {
        logger.info("Adding firewall entry");
        incomingEntries.add(entry);

        Connection con = databaseSettings.getDatabaseConnection();

        int pos = 1;
        PreparedStatement stm = con.prepareStatement(INSERT_ENTRY);
        stm.setString(pos++, entry.getLabel());
        stm.setString(pos++, entry.getDescription());
        stm.setBoolean(pos++, entry.getIsActive());
        stm.execute();
        ResultSet result = stm.getResultSet();
        if (result.next()) {
            entry.setId(result.getInt(1));
            addWhos(entry);
            addWheres(entry);
            addWhats(entry);
        }
        else {
            logger.info("No id returned");
        }
    }
}
