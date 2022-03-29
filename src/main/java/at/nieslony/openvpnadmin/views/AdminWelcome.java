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
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.ConfigurableNavigationHandler;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.MenuModel;

/**
 *
 * @author claas
 */
@SessionScoped
@Named
public class AdminWelcome implements Serializable {
    private MenuModel menuModel;
    DefaultSubMenu userVpnsMenu;
    DefaultSubMenu siteVpnsMenu;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    FolderFactory folderFactory;

    @Inject
    CurrentUser currentUser;

    @Inject
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
        DefaultSubMenu usersMenu = DefaultSubMenu.builder()
                .label("Users & Roles")
                .addElement(
                        DefaultMenuItem.builder()
                                .value("View/Edit users")
                                .url("EditUsers.xhtml")
                                .build()
                )
                .addElement(
                        DefaultMenuItem.builder()
                                .value("View/Edit roles")
                                .url("EditRoles.xhtml")
                                .build()
                )
                .addElement(
                        DefaultMenuItem.builder()
                                .value("Configure LDAP source")
                                .url("LdapSetup.xhtml")
                                .build()
                )
                .addElement(
                        DefaultMenuItem.builder()
                                .value("Edit authentication settings")
                                .url("EditAuthSettings.xhtml")
                                .build()
                )
                .build();

        DefaultSubMenu certsMenu = DefaultSubMenu.builder()
                .label("Certificates")
                .addElement(
                        DefaultMenuItem.builder()
                                .value("User Certificates")
                                .url("UserCertificates.xhtml")
                                .build()
                )
                .addElement(
                        DefaultMenuItem.builder()
                                .value("Edit client cert defaults")
                                .url("EditClientCertificateSettings.xhtml")
                                .build()
                )
                .addElement(
                        DefaultMenuItem.builder()
                                .value("Edit server cert defaults")
                                .url("EditServerCertificateSettings.xhtml")
                                .build()
                )
                .addElement(
                        DefaultMenuItem.builder()
                                .value("Renew server certificate")
                                .url("RenewServerCertificate.xhtml")
                                .build()
                )
                .addElement(
                        DefaultMenuItem.builder()
                                .value("Edit firewall zone")
                                .url("EditFirewallZone.xhtml")
                                .build()
                )
                .build();


        userVpnsMenu = DefaultSubMenu.builder()
                .label("User VPNs")
                .build();
        loadUserVpns();

        DefaultSubMenu actionsMenu = DefaultSubMenu.builder()
                .label("Actions")
                .addElement(
                        DefaultMenuItem.builder()
                                .value("Show user status")
                                .url("ShowUserStatus.xhtml")
                                .build()
                )
                .addElement(
                        DefaultMenuItem.builder()
                                .value("Edit schedules tasks")
                                .url("ScheduledTasks.xhtml")
                                .build()
                )
                .addElement(
                        DefaultMenuItem.builder()
                                .value("Logout…")
                                .command("#{adminWelcome.logout}")
                                .build()
                )
                .build();
        if (currentUser.hasRole("user")) {
            actionsMenu.getElements().add(0,
                    DefaultMenuItem.builder()
                            .value("Switch to user welcome")
                            .url("UserWelcome.xhtml")
                            .build()
            );
        }

        menuModel = new DefaultMenuModel();
        menuModel.getElements().add(usersMenu);
        menuModel.getElements().add(certsMenu);
        menuModel.getElements().add(userVpnsMenu);
        menuModel.getElements().add(actionsMenu);
    }

    public MenuModel getMenuModel() {
        return menuModel;
    }

    public void loadUserVpns() {
        logger.info("Loading user VPNs...");

        List<MenuElement> menuElements = userVpnsMenu.getElements();
        menuElements.clear();

        String vpnName = userVpn.getIsEnabled() ?
            userVpn.getConnectionName() : "Add user VPN";

        DefaultMenuItem addUserVpn = DefaultMenuItem.builder()
                .value(vpnName)
                .url("EditUserVPN.xhtml")
                .build();

        menuElements.add(addUserVpn);
    }

    public void logout() throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        ec.invalidateSession();
        ConfigurableNavigationHandler nav =
                (ConfigurableNavigationHandler)
                    fc.getApplication().getNavigationHandler();

        logger.info("Navigating to Login");
        nav.performNavigation("Login");
    }

    public void onAbout() {

    }
}
