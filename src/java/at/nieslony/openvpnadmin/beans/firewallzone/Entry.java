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
    private String label = "NewEntryRule";
    private String description = "";
    private boolean isActive = false;
    private List<Where> where = new LinkedList<>();
    private List<What> what = new LinkedList<>();
    private List<Who> who = new LinkedList<>();

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

    public String getWhoStr() {
        return "TBD";
    }

    public String getWhereStr() {
        List<String> whereStr = new LinkedList<>();
        where.forEach(w -> whereStr.add(w.toString()));

        return String.join(", ", whereStr);
    }

    public String getWhatStr() {
        List<String> whatStr = new LinkedList<>();
        what.forEach(w -> whatStr.add(w.toString()));

        return String.join(", ", whatStr);
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
}
