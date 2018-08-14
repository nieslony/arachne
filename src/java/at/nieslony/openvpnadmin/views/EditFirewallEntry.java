/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.RoleRuleFactoryCollection;
import at.nieslony.openvpnadmin.beans.firewallzone.Entry;
import at.nieslony.openvpnadmin.beans.firewallzone.What;
import at.nieslony.openvpnadmin.beans.firewallzone.What.WhatType;
import at.nieslony.openvpnadmin.beans.firewallzone.Where;
import at.nieslony.openvpnadmin.beans.firewallzone.Who;
import at.nieslony.openvpnadmin.views.editfirewallsettings.EditMode;
import at.nieslony.openvpnadmin.views.editfirewallsettings.EditWhat;
import at.nieslony.openvpnadmin.views.editfirewallsettings.EditWhere;
import at.nieslony.openvpnadmin.views.editfirewallsettings.EditWho;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
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

    final private EditWhat editWhat = new EditWhat(this);
    final private EditWhere editWhere = new EditWhere(this);
    final private EditWho editWho = new EditWho(this);

    private Where selectedWhere;

    @ManagedProperty(value = "#{roleRuleFactoryCollection}")
    RoleRuleFactoryCollection roleRuleFactoryCollection;
    public void setRoleRuleFactoryCollection(RoleRuleFactoryCollection rrfc) {
        roleRuleFactoryCollection = rrfc;
    }

    public RoleRuleFactoryCollection getRoleRuleFactoryCollection() {
        return roleRuleFactoryCollection;
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

        editWho.beginEdit(new Who(), EditMode.NEW);

        PrimeFaces.current().executeScript("PF('dlgEditWho').show();");
    }

    public void onAddWhere() {
        logger.info("Adding new where");

        editWhere.beginEdit(new Where(), EditMode.NEW);

        PrimeFaces.current().executeScript("PF('dlgEditWhere').show();");
    }

    public void onEditWho() {
        Who who = getWhoForId(selectedWhoId);
        if (who == null) {
            logger.info("No who selected for editing");
            return;
        }
        logger.info(String.format("Editing who %s", who.getAsString()));
        editWho.beginEdit(who, EditMode.MODIFY);
        PrimeFaces.current().executeScript("PF('dlgEditWho').show();");
    }

    public void onEditWhere() {
        Where where = getWhereForId(selectedWhereId);
        if (where == null) {
            logger.info("No where selected for editing");
            return;
        }
        logger.info(String.format("Editing where %s", where.getAsString()));
        editWhere.beginEdit(where, EditMode.MODIFY);
        PrimeFaces.current().executeScript("PF('dlgEditWhere').show();");
    }

    public void onEditWhat() {
        What what = getWhatForId(selectedWhatId);
        if (what == null) {
            logger.info("No what selected for editing");
            return;
        }
        logger.info(String.format("Editing where %s", what.getAsString()));
        editWhat.beginEdit(what, EditMode.MODIFY);
        PrimeFaces.current().executeScript("PF('dlgEditWhat').show();");
    }

    public EditWho getEditWho() {
        return editWho;
    }

    public EditWhere getEditWhere() {
        return editWhere;
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
}
