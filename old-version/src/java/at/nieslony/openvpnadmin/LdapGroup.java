/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
public class LdapGroup {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private String name;
    private String description;
    private final List<String> memberUids = new LinkedList<>();
    private final List<String> memberDNs = new LinkedList<>();

    public boolean hasMember(LdapUser user) {
        for (String m: memberUids) {
            if (m.equals(user.getUsername()))
                return true;
            logger.info(String.format("%s != %s", m, user.getUsername()));
        }
        for (String m: memberDNs) {
            if (m.equals(user.getDn()))
                return true;
            logger.info(String.format("%s != %s", m, user.getDn()));
        }
        return false;
    }

    public List<String> getMemberDNs() {
        return memberDNs;
    }

    public List<String> getMemberUids() {
        return memberUids;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
