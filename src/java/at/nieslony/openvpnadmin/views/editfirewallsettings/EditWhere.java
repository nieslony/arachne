/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views.editfirewallsettings;

import at.nieslony.openvpnadmin.beans.firewallzone.Where;
import at.nieslony.openvpnadmin.views.EditFirewallEntry;
import java.util.logging.Logger;
import org.primefaces.PrimeFaces;

/**
 *
 * @author claas
 */
public class EditWhere {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private Where where;
    private EditMode editMode;

    private EditFirewallEntry editFirewallEntry;

    Where.WhereType whereType;
    String hostname;
    String network = "0.0.0.0";
    int mask = 0;

    public EditWhere(EditFirewallEntry efe) {
        editFirewallEntry = efe;
    }

    public Where.WhereType getWhereType() {
        return whereType;
    }

    public void setWhereType(Where.WhereType wt) {
        whereType = wt;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hn) {
        hostname = hn;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String nw) {
        network = nw;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int m) {
        mask = m;
    }

    public void beginEdit(Where w, EditMode em) {
        where = w;
        loadWhere();
        editMode = em;
    }

    private void loadWhere() {
        whereType = where.getWhereType();
        hostname = where.getHostname();
        network = where.getNetwork();
        mask = where.getMask();
    }
        
    private void saveWhere() {
        where.setWhereType(whereType);
        where.setHostname(hostname);
        where.setNetwork(network);
        where.setMask(mask);

        switch (editMode) {
            case MODIFY:
                break;
            case NEW:
                editFirewallEntry.getWheres().add(where);
                break;
        }
    }

    public void onOk() {
        saveWhere();
        PrimeFaces.current().executeScript("PF('dlgEditWhere').hide();");
    }

    public void onCancel() {
        PrimeFaces.current().executeScript("PF('dlgEditWhere').hide();");
    }
}
