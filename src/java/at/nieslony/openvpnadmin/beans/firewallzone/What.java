/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans.firewallzone;

import at.nieslony.openvpnadmin.FirewallDService;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
public class What implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

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
    private int portFrom = 1;
    private int portTo = 65535;
    private int port = 1;
    private List<String> ports = new LinkedList<>();
    private Protocol protocol = Protocol.TCP;
    private FirewallDService service = null;
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
        ports.clear();
        ports.addAll(p);
    }

    public void setPort(int p) {
        port = p;
    }

    public int getPort() {
        return port;
    }

    public String getAsString() {
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
                buf.append(port).append(" / ").append(protocol);
                break;
            case PortRangeProtocol:
                buf.append(portFrom).append(" - ").append(portTo).append(" / ").append(protocol);
                break;
            case Service:
                if (service == null) {
                    buf.append("Service is null");
                }
                else {
                    buf.append(service.getShortDescription());
                }
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

    public FirewallDService getService() {
        return service;
    }

    public void setService(FirewallDService s) {
        service = s;
    }
}
