/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views.editfirewallsettings;

import at.nieslony.openvpnadmin.beans.firewallzone.Entry;
import at.nieslony.openvpnadmin.beans.firewallzone.What;
import at.nieslony.openvpnadmin.beans.firewallzone.What.WhatType;
import at.nieslony.openvpnadmin.beans.firewallzone.Where;
import at.nieslony.openvpnadmin.beans.firewallzone.Who;
import at.nieslony.openvpnadmin.views.EditFirewallSettings;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;
import org.primefaces.PrimeFaces;

/**
 *
 * @author claas
 */
public class EditFirewallEntry implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private final HashMap<String, Who> whos = new HashMap<>();
    private String selectedWhoId = null;

    private final HashMap<String, Where> wheres = new HashMap<>();
    private String selectedWhereId = null;

    private final HashMap<String, What> whats = new HashMap<>();
    private String selectedWhatId = null;

    private String label = "";
    private String description = "";
    private boolean isActive = false;

    private Entry firewallEntry;

    EditFirewallSettings editFirewallSettings;

    public EditFirewallEntry(EditFirewallSettings efs) {
        editFirewallSettings = efs;
    }

    public void setFirewallEntry(Entry e) {
        firewallEntry = e;

        readValues();
    }

    private void readValues() {
        whos.clear();
        firewallEntry.getWhos().forEach( w -> {
            addWho(w);
        });

        wheres.clear();
        firewallEntry.getWheres().forEach( w -> {
            addWhere(w);
        });

        whats.clear();
        firewallEntry.getWhats().forEach( w -> {
            addWhat(w);
        });

        label = firewallEntry.getLabel();
        description = firewallEntry.getDescription();
        isActive = firewallEntry.getIsActive();
    }

    public String getSelectedWhoId() {
        return selectedWhoId;
    }

    public void setSelectedWhoId(String id) {
        selectedWhoId = id;
    }

    public Who getWhoForId(String id) {
        return whos.get(id);
    }

    public Collection<String> getWhoIds() {
        return whos.keySet();
    }

    public String getSelectedWhereId() {
        return selectedWhereId;
    }

    public void setSelectedWhereId(String id) {
        selectedWhereId = id;
    }

    public Where getWhereForId(String id) {
        return wheres.get(id);
    }

    public Collection<String> getWhereIds() {
        return wheres.keySet();
    }

    public String getSelectedWhatId() {
        return selectedWhatId;
    }

    public void setSelectedWhatId(String id) {
        selectedWhatId = id;
    }

    public What getWhatForId(String id) {
        return whats.get(id);
    }

    public Collection<String> getWhatIds() {
        return whats.keySet();
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

    public void onAddWho() {
        logger.info("Adding new who");

        editFirewallSettings.getEditWho().beginEdit(new Who(), EditMode.NEW);

        PrimeFaces.current().executeScript("PF('dlgEditWho').show();");
    }

    public void onAddWhere() {
        logger.info("Adding new where");

        editFirewallSettings.getEditWhere().beginEdit(new Where(), EditMode.NEW);

        PrimeFaces.current().executeScript("PF('dlgEditWhere').show();");
    }

    public void onEditWho() {
        Who who = getWhoForId(selectedWhoId);
        if (who == null) {
            logger.info("No who selected for editing");
            return;
        }
        logger.info(String.format("Editing who %s", who.getAsString()));
        editFirewallSettings.getEditWho().beginEdit(who, EditMode.MODIFY);
        PrimeFaces.current().executeScript("PF('dlgEditWho').show();");
    }

    public void onEditWhere() {
        Where where = getWhereForId(selectedWhereId);
        if (where == null) {
            logger.info("No where selected for editing");
            return;
        }
        logger.info(String.format("Editing where %s", where.getAsString()));
        editFirewallSettings.getEditWhere().beginEdit(where, EditMode.MODIFY);
        PrimeFaces.current().executeScript("PF('dlgEditWhere').show();");
    }

    public void onEditWhat() {
        What what = getWhatForId(selectedWhatId);
        if (what == null) {
            logger.info("No what selected for editing");
            return;
        }
        logger.info(String.format("Editing where %s", what.getAsString()));
        editFirewallSettings.getEditWhat().beginEdit(what, EditMode.MODIFY);
        PrimeFaces.current().executeScript("PF('dlgEditWhat').show();");
    }

    public void onAddWhat() {
        logger.info("Adding new what");

        editFirewallSettings.getEditWhat().beginEdit(new What(), EditMode.NEW);

        PrimeFaces.current().executeScript("PF('dlgEditWhat').show();");
    }

    public void onRemoveWho() {
        if (selectedWhoId != null) {
            whos.remove(selectedWhoId);
            selectedWhoId = null;
        }
    }

    public void onRemoveWhere() {
        if (selectedWhereId != null) {
            wheres.remove(selectedWhereId);
            selectedWhereId = null;
        }
    }

    public void onRemoveWhat() {
        if (selectedWhatId != null) {
            whats.remove(selectedWhatId);
            selectedWhatId = null;
        }
    }

    public void onOk() {
        PrimeFaces.current().executeScript("PF('dlgEditFirewallEntry').hide();");
        editFirewallSettings.addIncomingEntry(firewallEntry);
    }

    public void onCancel() {
        PrimeFaces.current().executeScript("PF('dlgEditFirewallEntry').hide();");
    }

    public void addWho(Who who) {
        whos.put(String.valueOf(who.hashCode()), who);
    }

    public void addWhere(Where where) {
        wheres.put(String.valueOf(where.hashCode()), where);
    }

    public void addWhat(What what) {
        whats.put(String.valueOf(what.hashCode()), what);
    }

    public Collection<Who> getWhos() {
        return whos.values();
    }

    public boolean isValid() {
        boolean valid = !whos.isEmpty() && !wheres.isEmpty() && !whats.isEmpty();
        logger.info(String.format("Entry is %svalid",
                (valid ? "" : "not ")
        ));

        return valid;
    }
}
