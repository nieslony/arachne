/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans.firewallzone;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author claas
 */
public class Entry {
    private String label = "NewEntry";
    private String description = "";
    private boolean isActive = false;
    List<Target> targets = new LinkedList<>();
    List<String> who = new LinkedList<>();

    public String getLabel() {
        return label;
    }

    public void setLabel(String l) {
        label = l;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        description = d;
    }

    public String getWho() {
        return "";
    }

    public String getTarget() {
        return "";
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean ia) {
        isActive = ia;
    }

    public Entry clone() {
        Entry cloned = new Entry();

        return cloned;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public List<String> getWhos() {
        return who;
    }
}
