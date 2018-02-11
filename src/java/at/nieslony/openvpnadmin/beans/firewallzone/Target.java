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
public class Target {
    public enum Protocol {
        TCP, UDP
    }

    int portFrom;
    int portTo;
    String network;
    int mask;
    Protocol protocol;

    public int portFrom() {
        return portFrom;
    }

    public void setPortFrom(int p) {
        portFrom = p;
    }

    public int portTo() {
        return portTo;
    }

    public void setPortTo(int p) {
        portTo = p;
    }

    public String getPorts() {
        if (portFrom == portTo)
            return String.valueOf(portFrom);

        StringBuilder buf = new StringBuilder();
        buf.append(portFrom).append("-").append(portTo);

        return buf.toString();
    }

    public String getNetwork() {
        return network;
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

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol p) {
        protocol = p;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(network).append("/").append(mask)
                .append(":")
                .append(getPorts())
                .append("/").append(protocol);

        return buf.toString();
    }
}
