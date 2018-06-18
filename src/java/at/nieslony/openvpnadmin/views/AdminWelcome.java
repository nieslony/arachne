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

import at.nieslony.openvpnadmin.beans.CurrentUser;
import at.nieslony.openvpnadmin.beans.FolderFactory;
import at.nieslony.openvpnadmin.beans.UserVpn;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.DynamicMenuModel;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.MenuModel;

/**
 *
 * @author claas
 */
@ManagedBean
@SessionScoped
public class AdminWelcome implements Serializable {
    private MenuModel menuModel;
    DefaultSubMenu userVpnsMenu;
    DefaultSubMenu siteVpnsMenu;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{folderFactory}")
    FolderFactory folderFactory;

    @ManagedProperty(value = "#{currentUser}")
    CurrentUser currentUser;

    @ManagedProperty(value = "#{userVpn}")
    UserVpn userVpn;

    public void setUserVpn(UserVpn uv) {
        userVpn = uv;
    }

    public void setFolderFactory(FolderFactory ff) {
        this.folderFactory = ff;
    }

    public void setCurrentUser(CurrentUser cub) {
        currentUser = cub;
    }

    /**
     * Creates a new instance of AdminBean
     */
    public AdminWelcome() {
    }

    @PostConstruct
    public void init() {
        menuModel = new DynamicMenuModel();

        DefaultSubMenu usersMenu = new DefaultSubMenu("Users & roles");
            DefaultMenuItem editUsers = new DefaultMenuItem("View/Edit users");
            editUsers.setHref("EditUsers.xhtml");

            DefaultMenuItem editRoles = new DefaultMenuItem("View/Edit roles");
            editRoles.setHref("EditRoles.xhtml");

            DefaultMenuItem editLdapUsers = new DefaultMenuItem("Configure LDAP source");
            editLdapUsers.setHref("LdapSetup.xhtml");

            DefaultMenuItem editAuthSettings = new DefaultMenuItem("Edit authentication settings");
            editAuthSettings.setHref("EditAuthSettings.xhtml");

            usersMenu.addElement(editUsers);
            usersMenu.addElement(editRoles);
            usersMenu.addElement(editLdapUsers);
            usersMenu.addElement(editAuthSettings);
        menuModel.addElement(usersMenu);

        DefaultSubMenu certsMenu = new DefaultSubMenu("Certificates");
            DefaultMenuItem userCerts = new DefaultMenuItem("User Certificates");
            userCerts.setHref("UserCertificates.xhtml");

            DefaultMenuItem editClientCertSettings = new DefaultMenuItem("Edit client cert defaults");
            editClientCertSettings.setHref("EditClientCertificateSettings.xhtml");

            DefaultMenuItem editServerCertSettings = new DefaultMenuItem("Edit server cert defaults");
            editServerCertSettings.setHref("EditServerCertificateSettings.xhtml");

            DefaultMenuItem renewServerCertificate = new DefaultMenuItem("Renew server certificate");
            renewServerCertificate.setHref("RenewServerCertificate.xhtml");

            certsMenu.addElement(userCerts);
            certsMenu.addElement(editClientCertSettings);
            certsMenu.addElement(editServerCertSettings);
            certsMenu.addElement(renewServerCertificate);
        menuModel.addElement(certsMenu);

        userVpnsMenu = new DefaultSubMenu("User VPNs");
        loadUserVpns();
        menuModel.addElement(userVpnsMenu);

        /*
        siteVpnsMenu = new DefaultSubMenu("Site VPNs");
        loadSiteVpns();
        menuModel.addElement(siteVpnsMenu);
        */

        DefaultSubMenu actionsMenu = new DefaultSubMenu("Actions");
            if (currentUser.hasRole("user")) {
                DefaultMenuItem userWelcomeItem = new DefaultMenuItem("Switch to user welcome");
                userWelcomeItem.setHref("UserWelcome.xhtml");
                actionsMenu.addElement(userWelcomeItem);
            }
            DefaultMenuItem statusItem = new DefaultMenuItem("Show user status");
            statusItem.setHref("ShowUserStatus.xhtml");
            actionsMenu.addElement(statusItem);

            DefaultMenuItem schedulesTasksItem = new DefaultMenuItem("Edit schedules tasks");
            schedulesTasksItem.setHref("ScheduledTasks.xhtml");
            actionsMenu.addElement(schedulesTasksItem);

            DefaultMenuItem logoutItem = new DefaultMenuItem("Logout...");
            logoutItem.setCommand("#{adminWelcome.logout}");
            actionsMenu.addElement(logoutItem);
        menuModel.addElement(actionsMenu);
    }

    public MenuModel getMenuModel() {
        return menuModel;
    }

    public void loadSiteVpns() {
        List<MenuElement> items = new LinkedList<>();

        DefaultMenuItem addSiteVPN = new DefaultMenuItem("Add site VPN");
        items.add(addSiteVPN);

        siteVpnsMenu.setElements(items);
    }

    public void loadUserVpns() {
        logger.info("Loading user VPNs...");
        List<MenuElement> items = new LinkedList<>();

        String vpnName = userVpn.getIsEnabled() ?
            userVpn.getConnectionName() : "Add user VPN";

        DefaultMenuItem addUserVPN = new DefaultMenuItem(vpnName);
        addUserVPN.setHref("EditUserVPN.xhtml");
        items.add(addUserVPN);

        userVpnsMenu.setElements(items);
    }

    public void logout() throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        ec.invalidateSession();
        //ec.redirect(ec.getRequestContextPath() + "Login");
        ConfigurableNavigationHandler nav =
                (ConfigurableNavigationHandler)
                    fc.getApplication().getNavigationHandler();

        logger.info("Navigating to Login");
        nav.performNavigation("Login");
    }

    public void onAbout() {

    }
}
