/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views.editfirewallsettings;

import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.openvpnadmin.beans.RoleRuleFactory;
import at.nieslony.openvpnadmin.beans.firewallzone.Who;
import at.nieslony.openvpnadmin.views.EditFirewallEntry;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.primefaces.PrimeFaces;

/**
 *
 * @author claas
 */
public class EditWho implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private Who who;
    private EditMode editMode;
    private EditFirewallEntry editFirewallEntry;

    private String whoType = "";
    private String whoValue = "";

    public EditWho(EditFirewallEntry efe) {
        editFirewallEntry = efe;
    }

    public void beginEdit(Who w, EditMode em) {
        who = w;
        loadWho();
        editMode = em;
    }

    private void loadWho() {
        whoType = who.getWhoType();
        whoValue = who.getWhoValue();
    }

    private void saveWho() {
        RoleRule rr = editFirewallEntry.getRoleRuleFactoryCollection()
                .createRoleRule(whoType, whoValue);
        who.setTypeAndValue(rr);

        switch (editMode) {
            case MODIFY:
                logger.info(String.format("Saving who %s", who.getAsString()));
                break;
            case NEW:
                logger.info(String.format("Adding who %s", who.getAsString()));
                editFirewallEntry.getWhos().add(who);
                break;
            default:
                logger.warning("Invalid editMode");
        }
    }

    public String getWhoType() {
        return whoType;
    }

    public void setWhoType(String wt) {
        whoType = wt;
    }

    public String getWhoValue() {
        return whoValue;
    }

    public void setWhoValue(String wv) {
        whoValue = wv;
    }

    public void onOk() {
        saveWho();
        PrimeFaces.current().executeScript("PF('dlgEditWho').hide();");
    }

    public void onCancel() {
        PrimeFaces.current().executeScript("PF('dlgEditWho').hide();");
    }

    public List<String> onCompleteRoleValue(String pattern) {
        logger.info(String.format("Trying to complete %s", pattern));

        if (pattern == null)
            return null;

        RoleRuleFactory factory =
                editFirewallEntry.getRoleRuleFactoryCollection().getFactory(whoType);
        if (factory == null) {
            logger.warning(String.format("rule type %s doesn't exist", whoType));
            return new LinkedList<>();
        }
        return factory.completeValue(pattern);
    }
}
