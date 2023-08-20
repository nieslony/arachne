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
import javax.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        onRefresh();
    }

    List<ManagementInterface.UserStatus> userStatus = new LinkedList<>();
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    /**
     * Creates a new instance of ShowUserStatusBean
     */
    public ShowUserStatus() {
    }

    public List<ManagementInterface.UserStatus> getStatusEntries() {
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
