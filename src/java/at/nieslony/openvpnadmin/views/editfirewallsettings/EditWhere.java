/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views.editfirewallsettings;

import at.nieslony.openvpnadmin.beans.firewallzone.What;
import at.nieslony.openvpnadmin.views.EditFirewallEntry;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
public class EditWhere {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private What.WhatType whatType = What.WhatType.Service;

    private EditFirewallEntry editFirewallEntry;

    public EditWhere(EditFirewallEntry efe) {
        editFirewallEntry = efe;
    }


}
