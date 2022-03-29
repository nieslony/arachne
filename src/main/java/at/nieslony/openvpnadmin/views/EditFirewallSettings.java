/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.FirewallSettings;
import at.nieslony.openvpnadmin.beans.RoleRuleFactoryCollection;
import at.nieslony.openvpnadmin.beans.firewallzone.Entry;
import at.nieslony.openvpnadmin.views.editfirewallsettings.EditFirewallEntry;
import at.nieslony.openvpnadmin.views.editfirewallsettings.EditWhat;
import at.nieslony.openvpnadmin.views.editfirewallsettings.EditWhere;
import at.nieslony.openvpnadmin.views.editfirewallsettings.EditWho;
import at.nieslony.openvpnadmin.views.editfirewallsettings.FirewallEntryInfo;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.primefaces.PrimeFaces;

/**
 *
 * @author claas
 */
@ViewScoped
@Named
public class EditFirewallSettings implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    final static String CHAIN_INCOMING = "incoming";
    final static String CHAIN_OUTGOING = "outgoing";


    final List<FirewallEntryInfo> incomingEntries = new LinkedList<>();

    final EditFirewallEntry editFirewallEntry = new EditFirewallEntry(this);
    final EditWho editWho = new EditWho(this);
    final EditWhere editWhere = new EditWhere(this);
    final EditWhat editWhat = new EditWhat(this);

    public EditFirewallEntry getEditFirewallEntry() {
        return editFirewallEntry;
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

    @Inject
    private FirewallSettings firewallSettings;
    public void setFirewallSettings(FirewallSettings fs) {
        firewallSettings = fs;
    }

    @Inject
    RoleRuleFactoryCollection roleRuleFactoryCollection;
    public void setRoleRuleFactoryCollection(RoleRuleFactoryCollection rrfc) {
        roleRuleFactoryCollection = rrfc;
    }

    public RoleRuleFactoryCollection getRoleRuleFactoryCollection() {
        return roleRuleFactoryCollection;
    }

    public enum EditMode {
        EM_NEW("New"),
        EM_CLONE("Clone"),
        EM_EDIT("Edit");

        String mode;

        EditMode(String m) {
            mode = m;
        }

        @Override
        public String toString() {
            return mode;
        }

        public String getAsString() {
            return mode;
        }
    }

    String editingChain;
    EditMode editingMode;
    FirewallEntryInfo selectedIncomingEntry;
    Entry editingEntry;

    public EditFirewallSettings() {
    }

    @PostConstruct
    public void init() {
        onSyncEntries();
    }

    public void onSyncEntries() {
        incomingEntries.clear();
        firewallSettings.getIncomingEntries().forEach(
                e -> incomingEntries.add(new FirewallEntryInfo(e))
        );
    }

    public void onNewIncomingEntry() {
        logger.info("Open Entry Dialog");

        editingChain = CHAIN_INCOMING;
        editingMode = EditMode.EM_NEW;

        editingEntry = new Entry();

        editFirewallEntry.setFirewallEntry(editingEntry);

        PrimeFaces.current().executeScript("PF('dlgEditFirewallEntry').show();");
    }

    public void onCloneIncomingEntry() {
        editingChain = CHAIN_INCOMING;
        editingMode = EditMode.EM_CLONE;

        PrimeFaces.current().executeScript("PF('dlgEditFirewallEntry').show();");
    }

    public void onEditIncomingEntry() {
        editingChain = CHAIN_INCOMING;
        editingMode = EditMode.EM_EDIT;

        editingEntry = selectedIncomingEntry.getEntry();
        editFirewallEntry.setFirewallEntry(editingEntry);

        PrimeFaces.current().executeScript("PF('dlgEditFirewallEntry').show();");
    }

    public void onRemoveIncomingEntry() {
        try {
            incomingEntries.remove(selectedIncomingEntry);
            firewallSettings.removeIncomingEntry(selectedIncomingEntry.getEntry());
            selectedIncomingEntry = null;
        }
        catch (ClassNotFoundException | SQLException ex) {
            String msg = String.format("Cannot remove firewall entry: %s", ex.getMessage());
            logger.warning(msg);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
        }
    }

    public void onEditEntryOk() {
        editingEntry = null;

        PrimeFaces.current().ajax().update(":formEditFirewallEntry");
        PrimeFaces.current().executeScript("PF('dlgEditFirewallEntry').hide();");
    }

    public void onEditEntryCancel() {
        editingEntry = null;

        PrimeFaces.current().executeScript("PF('dlgEditFirewallEntry').hide();");
    }

    public String getEditingChain() {
        return editingChain;
    }

    public EditMode getEditingMode() {
        return editingMode;
    }

    public Entry getEditingEntry() {
        if (editingEntry == null && editingMode != null) {
            switch (editingMode) {
                case EM_NEW:
                    editingEntry = new Entry();
                    break;
                case EM_EDIT:
                    editingEntry = selectedIncomingEntry.getEntry();
                case EM_CLONE:
                    editingEntry = selectedIncomingEntry.getEntry().clone();
            }
        }

        return editingEntry;
    }

    public void setSelectedIncomingEntry(FirewallEntryInfo e) {
        selectedIncomingEntry = e;
    }

    public FirewallEntryInfo getSelectedIncomingEntry() {
        return selectedIncomingEntry;
    }

    public List<FirewallEntryInfo> getIncomingEntries() {
        return incomingEntries;
    }

    public void updateIncomingEntry(Entry entry) {
        logger.info(String.format("Updating %s entry %s",
                (entry.getIsActive() ? "active" : "inactive"),
                entry.getLabel()
        ));
        try {
            firewallSettings.updateIncomingEntry(entry);
        }
        catch (ClassNotFoundException | SQLException ex) {
            String msg = String.format("Cannot update firewall entry: %s", ex.getMessage());
            logger.warning(msg);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
        }
    }

    public void addIncomingEntry(Entry entry) {
        logger.info(String.format("Adding %s entry %s",
                (entry.getIsActive() ? "active" : "inactive"),
                entry.getLabel()
        ));
        try {
            firewallSettings.addIncomingEntry(entry);
            incomingEntries.add(new FirewallEntryInfo(entry));
        }
        catch (ClassNotFoundException | SQLException ex) {
            String msg = String.format("Cannot add firewall entry: %s", ex.getMessage());
            logger.warning(msg);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
        }
    }
}
