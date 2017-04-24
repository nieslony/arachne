/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

            certsMenu.addElement(userCerts);
            certsMenu.addElement(editClientCertSettings);
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
}
