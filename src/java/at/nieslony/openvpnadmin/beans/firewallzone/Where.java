/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans.firewallzone;

/**
 *
 * @author claas
 */
public class Where {
    String network = "0.0.0.0";
    int mask = 0;

    public String getNetwork() {
        return network;
    }

    public String getAsString() {
        StringBuilder buf = new StringBuilder();
        switch (mask) {
            case 0:
                buf.append("Evewhere");
                break;
            case 32:
                buf.append("Host ").append(network);
                break;
            default:
                buf.append("Network ").append(network).append(" / ").append(mask);
                break;
        }

        return buf.toString();
    }

    public void setNetwork(String n) {
        network = n;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int m) {
        mask = m;
    }
}
