/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views.editfirewallsettings;

import at.nieslony.openvpnadmin.beans.firewallzone.Who;
import at.nieslony.openvpnadmin.views.EditFirewallEntry;
import java.io.Serializable;
import org.primefaces.PrimeFaces;

/**
 *
 * @author claas
 */
public class EditWho implements Serializable {
    private Who who;
    private EditMode editMode;
    private EditFirewallEntry editFirewallEntry;

    public EditWho(EditFirewallEntry efe) {
        editFirewallEntry = efe;
    }

    public void beginEdit(Who w, EditMode em) {
        who = w;
        loadWho();
        editMode = em;
    }

    private void loadWho() {

    }

    private void saveWho() {

    }

    public void onOk() {
        saveWho();
        PrimeFaces.current().executeScript("PF('dlgEditWho').hide();");
    }

    public void onCancel() {
        PrimeFaces.current().executeScript("PF('dlgEditWho').hide();");
    }
}
