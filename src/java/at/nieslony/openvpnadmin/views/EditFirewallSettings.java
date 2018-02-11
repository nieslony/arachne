/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.firewallzone.Entry;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.primefaces.context.RequestContext;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class EditFirewallSettings implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    final static String CHAIN_INCOMING = "incoming";
    final static String CHAIN_OUTGOING = "outgoing";

    enum EditMode {
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
    }

    String editingChain;
    EditMode editingMode;
    Entry editingEntry;
    Entry selectedIncomingEntry;

    public EditFirewallSettings() {
    }

    public void onNewIncomingEntry() {
        logger.info("Open Entry Dialog");

        editingChain = CHAIN_INCOMING;
        editingMode = EditMode.EM_NEW;

        editingEntry = new Entry();
        editingEntry.setLabel("NewEntry");

        RequestContext rctx = RequestContext.getCurrentInstance();
        rctx.execute("PF('dlgEditFirewallEntry').show();");
    }

    public void onCloneIncomingEntry() {
        editingChain = CHAIN_INCOMING;
        editingMode = EditMode.EM_CLONE;

        RequestContext rctx = RequestContext.getCurrentInstance();
        rctx.execute("PF('dlgEditFirewallEntry').show();");
    }

    public void onEditIncomingEntry() {
        editingChain = CHAIN_INCOMING;
        editingMode = EditMode.EM_EDIT;

        RequestContext rctx = RequestContext.getCurrentInstance();
        rctx.execute("PF('dlgEditFirewallEntry').show();");
    }

    public void onRemoveIncomingEntry() {
    }

    public void onEditEntryOk() {
        RequestContext rctx = RequestContext.getCurrentInstance();

        editingEntry = null;

        rctx.update(":formEditFirewallEntry");
        rctx.execute("PF('dlgEditFirewallEntry').hide();");
    }

    public void onEditEntryCancel() {
        RequestContext rctx = RequestContext.getCurrentInstance();

        editingEntry = null;

        rctx.execute("PF('dlgEditFirewallEntry').hide();");
    }

    public String getEditingChain() {
        return editingChain;
    }

    public String getEditingMode() {
        if (editingMode != null)
            return editingMode.toString();
        return "???";
    }

    public Entry getEditingEntry() {
        if (editingEntry == null && editingMode != null) {
            switch (editingMode) {
                case EM_NEW:
                    editingEntry = new Entry();
                    break;
                case EM_EDIT:
                    editingEntry = selectedIncomingEntry;
                case EM_CLONE:
                    editingEntry = selectedIncomingEntry.clone();
            }
        }

        return editingEntry;
    }

    public void setSelectedIncomingEntry(Entry e) {
        selectedIncomingEntry = e;
    }

    public Entry getSekectedIncomingEntry() {
        return selectedIncomingEntry;
    }
}
