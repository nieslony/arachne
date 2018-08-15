/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.beans.firewallzone.Entry;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

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

    public FirewallSettings() {
        incomingEntries = new LinkedList<>();
        outgoingEntries = new LinkedList<>();
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
