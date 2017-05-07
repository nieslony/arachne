/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.ManagementInterface;
import at.nieslony.openvpnadmin.exceptions.ManagementInterfaceException;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class ShowUserStatus implements Serializable {
    @ManagedProperty(value = "#{managementInterface}")
    private ManagementInterface managementInterface;

    public void setManagementInterface(ManagementInterface mi) {
        managementInterface = mi;
    }

    public class StatusEntry {

        final private String commonName;
        final private String remoteIP;
        final private String bytesReceived;
        final private String bytesSent;
        final private String connectedSince;

        public StatusEntry(String statusLine) {
            logger.fine(String.format("Parsing line %s", statusLine));
            // claas,89.144.222.73:37880,34730,10740,Fri Oct 30 21:57:44 2015
            String[] split = statusLine.split(",");
            commonName = split[0];
            remoteIP = split[1].split(":")[0];
            bytesReceived = split[2];
            bytesSent = split[3];
            connectedSince = split[4];
        }

        public String getCommonName() {
            return commonName;
        }

        public String getRemoteIP() {
            return remoteIP;
        }

        public String getBytesReceived() {
            return bytesReceived;
        }

        public String getBytesSent() {
            return bytesSent;
        }

        public String getConnectedSince() {
            return connectedSince;
        }
    }

    List<StatusEntry> statusEntries = new LinkedList<>();
    List<ManagementInterface.UserStatus> userStatus = new LinkedList<>();
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    /**
     * Creates a new instance of ShowUserStatusBean
     */
    public ShowUserStatus() {
    }

    public List<ManagementInterface.UserStatus> getStatusEntries() {
        //return statusEntries;
        return userStatus;
    }

    public void onRefresh()
    {
        try {
            userStatus.clear();
            managementInterface.getStatus(userStatus);
        }
        catch (IOException | ManagementInterfaceException ex) {
            String msg = String.format("Cannot refresh user status: %s", ex.getMessage());

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
        }
    }
}
