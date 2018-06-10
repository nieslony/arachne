/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans.firewallzone;

import java.io.Serializable;

/**
 *
 * @author claas
 */
public class Where implements Serializable {
    public enum WhereType {
        Everywhere("Everywhere"),
        Hostname("Hostname"),
        Network("Network");

        public final String _description;

        WhereType(String d) {
            _description = d;
        }

        public String getDescription() {
            return _description;
        }
    }

    private WhereType whereType = WhereType.Everywhere;
    String network = "0.0.0.0";
    int mask = 0;
    String hostname = "";

    public WhereType getWhereType() {
        return whereType;
    }

    public void setWhereType(WhereType wt) {
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

    public String getAsString() {
        StringBuilder buf = new StringBuilder();

        switch (whereType) {
            case Everywhere:
                buf.append("Everywhere");
                break;
            case Hostname:
                buf.append("Hostname ").append(hostname);
                break;
            case Network:
                switch (mask) {
                    case 0:
                        buf.append("All networks");
                        break;
                    case 32:
                        buf.append("Host ").append(network);
                        break;
                    default:
                        buf.append("Network ").append(network).append(" / ").append(mask);
                        break;
                }
                break;
        }

        return buf.toString();
    }

    public void setNetwork(String n) {
        network = n;    }

    public int getMask() {
        return mask;
    }

    public void setMask(int m) {
        mask = m;
    }
}
