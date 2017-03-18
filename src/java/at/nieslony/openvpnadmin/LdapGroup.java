/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author claas
 */
public class LdapGroup {
    private String name;
    private String description;
    private final List<String> memberUids = new LinkedList<>();
    private final List<String> memberDNs = new LinkedList<>();

    public boolean hasMember(VpnUser user) {
        for (String m: memberUids) {
            if (m.equals(user.getUsername()))
                return true;
        }
        for (String m: memberDNs) {
            if (m.equals(user.getDn()))
                return true;
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
