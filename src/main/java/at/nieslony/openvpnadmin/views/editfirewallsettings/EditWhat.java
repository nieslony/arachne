/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views.editfirewallsettings;

import at.nieslony.openvpnadmin.FirewallDService;
import at.nieslony.openvpnadmin.beans.firewallzone.What;
import at.nieslony.openvpnadmin.views.EditFirewallSettings;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.primefaces.PrimeFaces;

/**
 *
 * @author claas
 */
public class EditWhat implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private What.WhatType whatType = What.WhatType.Service;
    private FirewallDService service;
    private int port = 1;
    private int portFrom = 1;
    private int portTo = 65535;
    private What.Protocol protocol = What.Protocol.TCP;
    private final List<Integer> ports = new LinkedList<>();

    private What what;
    private EditMode editMode;

    private EditFirewallSettings editFirewallSettings;

    public EditWhat(EditFirewallSettings efs) {
        editFirewallSettings = efs;
    }

    public void beginEdit(What w, EditMode em) {
        what = w;
        loadWhat();
        editMode = em;
    }

    public FirewallDService getService() {
        return service;
    }

    public void setService(FirewallDService s) {
        service = s;
    }

    public void loadWhat() {
        port = what.getPort();
        whatType = what.getWhatType();
        portFrom = what.getPortFrom();
        portTo = what.getPortTo();
        protocol = what.getProtocol();
        ports.clear();
        ports.addAll(what.getPorts());
        service = what.getService();
    }

    public void saveWhat() {
        what.setPortFrom(portFrom);
        what.setPortTo(portTo);
        what.setPorts(ports);
        what.setPort(port);
        what.setProtocol(protocol);
        what.setService(service);
        what.setWhatType(whatType);

        switch (editMode) {
            case MODIFY:
                break;
            case NEW:
                editFirewallSettings.getEditFirewallEntry().addWhat(what);
                break;
        }
    }

    public What.WhatType getType() {
        return whatType;
    }

    public void setType(What.WhatType t) {
        whatType = t;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int p) {
        port = p;
    }

    public int getPortFrom() {
        return portFrom;
    }

    public void setPortFrom(int p) {
        portFrom = p;
    }

    public int getPortTo() {
        return portTo;
    }

    public void setPortTo(int p) {
        portTo = p;
    }

    public What.Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(What.Protocol p) {
        protocol = p;
    }

    public String getPorts() {
        List<String> tmp = new LinkedList<>();
        ports.forEach(p -> tmp.add(String.valueOf(p)));
        return String.join(" ", tmp);
    }

    public void setPorts(String p) {
        String[] tmp = p.split("[ ,;]+");
        ports.clear();
        for (String s : tmp) {
            try {
                int i = Integer.valueOf(s);
                if (i > 0 && i < 65535)
                    ports.add(Integer.valueOf(s));
            }
            catch (NumberFormatException ex) {
                logger.warning(ex.getMessage());
            }
        }
    }

    public void onOk() {
        saveWhat();
        PrimeFaces.current().executeScript("PF('dlgEditWhat').hide();");
    }

    public void onCancel() {
        PrimeFaces.current().executeScript("PF('dlgEditWhat').hide();");
    }
}