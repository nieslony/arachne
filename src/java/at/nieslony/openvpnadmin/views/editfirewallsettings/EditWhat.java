/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views.editfirewallsettings;

import at.nieslony.openvpnadmin.FirewallDService;
import at.nieslony.openvpnadmin.beans.firewallzone.What;
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
public class EditWhat implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private What.WhatType whatType = What.WhatType.Service;
    private FirewallDService service;
    private int port = 1;
    private int portFrom = 1;
    private int portTo = 65535;
    private What.Protocol protocol = What.Protocol.TCP;
    private final List<String> ports = new LinkedList<>();

    private What what;
    private EditMode editMode;

    private EditFirewallEntry editFirewallEntry;

    public EditWhat(EditFirewallEntry efe) {
        editFirewallEntry = efe;
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
                editFirewallEntry.addWhat(what);
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

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> p) {
        ports.clear();
        if (p != null)
            ports.addAll(p);
    }

    public void onOk() {
        saveWhat();
        PrimeFaces.current().executeScript("PF('dlgEditWhat').hide();");
    }

    public void onCancel() {
        PrimeFaces.current().executeScript("PF('dlgEditWhat').hide();");
    }
}
