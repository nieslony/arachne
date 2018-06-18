/*
 * Copyright (C) 2018 Claas Nieslony
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
