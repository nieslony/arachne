/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.firewallzone.Entry;
import at.nieslony.openvpnadmin.beans.firewallzone.What;
import at.nieslony.openvpnadmin.beans.firewallzone.What.WhatType;
import at.nieslony.openvpnadmin.beans.firewallzone.Where;
import at.nieslony.openvpnadmin.views.editfirewallsettings.EditMode;
import at.nieslony.openvpnadmin.views.editfirewallsettings.EditWhat;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.primefaces.PrimeFaces;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class EditFirewallEntry implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private final List<Where> wheres = new LinkedList<>();
    private final List<What> whats = new LinkedList<>();
    private String label = "";
    private String description = "";
    private boolean isActive = false;

    private Entry firewallEntry;

    final private EditWhat editWhat = new EditWhat(this);

    private Where selectedWhere;

   public void setFirewallEntry(Entry e) {
        firewallEntry = e;

        readValues();
    }

    private void readValues() {
        wheres.clear();
        wheres.addAll(firewallEntry.getWheres());
        whats.clear();
        whats.addAll(firewallEntry.getWhats());
        label = firewallEntry.getLabel();
        description = firewallEntry.getDescription();
        isActive = firewallEntry.getIsActive();
    }

    public void setSelectedWhere(Where sw) {
        selectedWhere = sw;
    }

    public Where getSelectedWhare() {
        return selectedWhere;
    }

    public List<Where> getWheres() {
        return wheres;
    }

    public List<What> getWhats() {
        return whats;
    }

    public WhatType[] getWhatTypes() {
        return WhatType.values();
    }

    public void setLabel(String l) {
        label = l;
    }

    public String getLabel() {
        return label;
    }

    public void setDescription(String d) {
        description = d;
    }

    public String getDescription() {
        return description;
    }

    public void setIsActive(boolean ia) {
        isActive = ia;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void onAddWhere() {

    }

    public EditWhat getEditWhat() {
        return editWhat;
    }

    public void updateEditWhat() {
    }

    public void onAddWhat() {
        logger.info("Adding new what");

        editWhat.beginEdit(new What(), EditMode.NEW);

        PrimeFaces.current().executeScript("PF('dlgEditWhat').show();");
    }

    public void onRemoveWhere() {

    }

    public void onRemoveWhat() {
    }

    public void onOk() {
        PrimeFaces.current().executeScript("PF('dlgEditFirewallEntry').hide();");
    }

    public void onCancel() {
        PrimeFaces.current().executeScript("PF('dlgEditFirewallEntry').hide();");
    }
}
