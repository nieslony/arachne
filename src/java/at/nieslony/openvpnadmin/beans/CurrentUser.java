/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.VpnUser;
import at.nieslony.openvpnadmin.exceptions.InvalidUsernameOrPassword;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapUser;
import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import java.io.Serializable;
import java.util.Base64;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author claas
 */
@ManagedBean
@SessionScoped
public class CurrentUser implements Serializable {
    private VpnUser vpnUser = null;

    @ManagedProperty(value = "#{ldapSettings}")
    private LdapSettings ldapSettings;

    @ManagedProperty(value = "#{localUsers}")
    private LocalUsers localUsers;

    @ManagedProperty(value = "#{roles}")
    private Roles roles;

    @ManagedProperty(value = "#{navigationBean}")
    private NavigationBean navigationBean;

    @ManagedProperty(value = "#{authSettings}")
    private AuthSettings authSettings;

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    /**
     * Creates a new instance of CurrentUserBean
     */
    public CurrentUser() {
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing currentUser");

        HttpServletRequest req = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String[] attributes = { "REMOTE_USER" };
        String[] headers = { "REMOTE_USER" };

        try {
            if (authSettings.getEnableAjpRemoteUser() && req.getRemoteUser() != null) {
                vpnUser = ldapSettings.findVpnUser(req.getRemoteUser());
            }
/*            else {
                for (String a: attributes) {
                    String remUsr = (String) req.getAttribute(a);
                    if (remUsr != null)
                        vpnUser = ldapSettings.findVpnUser(remUsr);
                    }
            }*/
            if (vpnUser == null && authSettings.getEnableHttpHeaderAuth()) {
                String remUser = (String) req.getHeader(authSettings.getHttpHeaderRemoteUser());
                if (remUser != null) {
                    vpnUser = ldapSettings.findVpnUser(remUser);
                }
            }
            if (vpnUser == null) {
                if (req.getHeader("authorization") != null) {
                    String auth[] = req.getHeader("authorization").split(" ");
                    if (auth.length == 2 && auth[0].equals("Basic")) {
                        byte[] decoded = Base64.getDecoder().decode(auth[1]);
                        String[] usrPwd = new String(decoded).split(":");
                        if (usrPwd.length == 2) {
                            vpnUser = localUsers.auth(usrPwd[0], usrPwd[1]);
                        }
                    }
                    throw new InvalidUsernameOrPassword();
                }
            }
        }
        catch (InvalidUsernameOrPassword iuop) {
            logger.severe(String.format("Illegal REMOTE_USER or password provided: %s",
                    iuop.getMessage()));
            // retirect to 403
        }
        catch (NoSuchLdapUser nslu) {
            logger.severe(nslu.getMessage());
        }
        catch (NamingException ne) {
            logger.severe(String.format("Error connecting to LDAP server: %s", ne.getMessage()));
        }
    }

    public boolean isValid() {
        return vpnUser != null && vpnUser.getUserType() != VpnUser.UserType.UT_UNASSIGNED;
    }

    public void isValid(ComponentSystemEvent event) {
        if (vpnUser == null) {
            FacesContext fc = FacesContext.getCurrentInstance();
            navigationBean.toLoginPage();
        }
    }

    public void setNavigationBean(NavigationBean nb) {
        navigationBean = nb;
    }

    public void setLocalUsers(LocalUsers lu) {
        localUsers = lu;
    }

    public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    public void setRoles(Roles r) {
        roles = r;
    }

    public String getUsername() {
        if (vpnUser != null)
            return vpnUser.getUsername();
        else
            return "n/a";
    }

    public String getSurname() {
        if (vpnUser != null)
            return vpnUser.getSurname();
        else
            return "";
    }

    public String getFullName() {
        if (vpnUser != null)
            return vpnUser.getFullName();
        else
            return "";
    }

    public String getGivename() {
        if (vpnUser != null)
            return vpnUser.getGivenName();
        else
            return "";
    }

    public String getVisibleName() {
        if (vpnUser == null)
            return "unknown";
        String name;
        name = vpnUser.getFullName();
        if (name != null && !name.isEmpty())
            return name;
        return vpnUser.getUsername();
    }

    public boolean hasRole(String rolename) {
	FacesContext fc = FacesContext.getCurrentInstance();

        if (vpnUser == null) {
            logger.info(String.format("There's no current user => no %s role", rolename));
            navigationBean.toLoginPage();
        }
        else if (!roles.hasUserRole(vpnUser.getUsername(), rolename)) {
            logger.info(String.format("User %s doesn't have role %s",
            vpnUser.getUsername(), rolename));
            return false;
        }

        return true;
    }

    public void redirectToWelcomePage(ComponentSystemEvent event)
            throws PermissionDenied
    {
        if (isValid())
            navigationBean.toWelcomePage(vpnUser);
        else
           navigationBean.toLoginPage();
    }

    public void isAdmin(ComponentSystemEvent event)
            throws PermissionDenied
    {
        if (!hasRole("admin")) {
            throw new PermissionDenied("User " + getUsername() + " doesn't have role admin");
        }
        else
            logger.info(String.format("User %s has required role admin", getUsername()));
    }

    public void isUser(ComponentSystemEvent event)
            throws PermissionDenied
    {
        if (!hasRole("user")) {
            throw new PermissionDenied("User " + getUsername() + " doesn't have role user");
        }
        else
            logger.info(String.format("User %s has required role user", getUsername()));
   }

    public void setLocalUser(VpnUser vu) {
        vpnUser = vu;
    }

    public void setAuthSettings(AuthSettings as) {
        authSettings = as;
    }
}
