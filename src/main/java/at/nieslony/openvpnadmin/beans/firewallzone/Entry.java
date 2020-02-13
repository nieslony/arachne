/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans.firewallzone;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author claas
 */
public class Entry implements Serializable {
    int id = -1;
    private String label = "NewEntryRule";
    private String description = "";
    private boolean isActive = false;
    private final List<Where> where = new LinkedList<>();
    private final List<What> what = new LinkedList<>();
    private final List<Who> who = new LinkedList<>();

    public Entry() {
    }

    public Entry(int id, String label, String description, boolean isActive) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.isActive = isActive;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public List<Where> getWheres() {
        return where;
    }

    public List<What> getWhats() {
        return what;
    }

    public List<Who> getWhos() {
        return who;
    }

    public void addWhere(Where w) {
        where.add(w);
    }

    public void removeWhere(Where w) {
        where.remove(w);
    }

    public boolean matchesUser(String username) {
        return true;
    }
}
