/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans.firewallzone;

import at.nieslony.openvpnadmin.FirewallDServices;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author claas
 */
public class What {
    public enum WhatType {
        Everything("Everything"),
        Service("Service"),
        PortProtocol("Port/Protocol"),
        PortRangeProtocol("Port range/protocol"),
        PortListProtocol("Port list/protocol");

        final private String _description;

        WhatType(String description) {
            _description = description;
        }

        public String getDescription() {
            return _description;
        }
    }

    public enum Protocol {
        TCP("Tcp"), UDP("Udp");

        final private String name;

        Protocol(String s) {
            name = s;
        }

        public String getName() {
            return name;
        }
    }

    private WhatType whatType = WhatType.Everything;
    private int portFrom;
    private int portTo;
    private List<String> ports = new LinkedList<>();
    private Protocol protocol;
    private FirewallDServices.Service service;
    private int id = -1;
    private static int tmpId = -1;

    public What() {
        tmpId--;
    }

    public int getId() {
        return id == -1 ? tmpId : id;
    }

    public void setId(int i) {
        id = i;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol p) {
        protocol = p;
    }

    public WhatType getWhatType() {
        return whatType;
    }

    public void setWhatType(WhatType wt) {
        whatType = wt;
    }

    public int getPortFrom() {
        return portFrom;
    }

    public void setPortFrom(int pf) {
        portFrom = pf;
    }

    public int getPortTo() {
        return portTo;
    }

    public void setPortTo(int pt) {
        portTo = pt;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> p) {
        ports = p;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        switch (whatType) {
            case Everything:
                buf.append("Everything");
                break;
            case PortListProtocol:
                buf.append(String.join(", ", ports));
                buf.append(" / ").append(protocol);
                break;
            case PortProtocol:
                buf.append(portFrom).append(" / ").append(protocol);
                break;
            case PortRangeProtocol:
                buf.append(portFrom).append(" - ").append(portTo).append(" / ").append(protocol);
                break;
            case Service:
                buf.append(service.getShortDescription());
                break;
        }

        return buf.toString();
    }

    public WhatType[] getAllWhatTypes() {
        return WhatType.values();
    }

    public Protocol[] getAllProtocols() {
        return Protocol.values();
    }

    public FirewallDServices.Service getService() {
        return service;
    }

    public void setService(FirewallDServices.Service s) {
        service = s;
    }
}
