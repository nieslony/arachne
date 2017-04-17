/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.AbstractUser;
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
    //private VpnUser vpnUser = null;
    private AbstractUser user = null;

    @ManagedProperty(value = "#{ldapSettings}")
    private LdapSettings ldapSettings;

    @ManagedProperty(value = "#{roles}")
    private Roles roles;

    @ManagedProperty(value = "#{navigationBean}")
    private NavigationBean navigationBean;

    @ManagedProperty(value = "#{authSettings}")
    private AuthSettings authSettings;

    @ManagedProperty(value = "#{localUserFactory}")
    LocalUserFactory localUserFactory;

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
        user = null;

        try {
            if (authSettings.getEnableAjpRemoteUser()) {
                logger.info("AJP remoteUser enabled");
                if (req.getRemoteUser() != null) {
                    try {
                        user = ldapSettings.findVpnUser(req.getRemoteUser());
                    }
                    catch (NamingException | NoSuchLdapUser ex) {
                        logger.info(String.format("Cannot find LDAP user %s: %s", ex.getMessage()));
                    }
                }
            }
            else {
                logger.info("AJP remoteUser disabled");
            }
            if (user ==  null) {
                if (req.getHeader("authorization") != null) {
                    String auth[] = req.getHeader("authorization").split(" ");
                    if (auth.length == 2 && auth[0].equals("Basic")) {
                        byte[] decoded = Base64.getDecoder().decode(auth[1]);
                        String[] usrPwd = new String(decoded).split(":");
                        if (usrPwd.length == 2) {
                            AbstractUser tmpUser = localUserFactory.findUser(usrPwd[0]);
                            if (tmpUser.auth(usrPwd[1])) {
                                user = tmpUser;
                            }
                            else {
                                throw new InvalidUsernameOrPassword();
                            }
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
    }

    public boolean isValid() {
        return user != null;
    }

    public void isValid(ComponentSystemEvent event) {
        if (user == null) {
            FacesContext fc = FacesContext.getCurrentInstance();
            navigationBean.toLoginPage();
        }
    }

    public void setNavigationBean(NavigationBean nb) {
        navigationBean = nb;
    }

    public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    public void setRoles(Roles r) {
        roles = r;
    }

    public String getUsername() {
        if (user != null)
            return user.getUsername();
        else
            return "n/a";
    }

    public String getSurname() {
        if (user != null)
            return user.getSurName();
        else
            return "";
    }

    public String getFullName() {
        if (user != null)
            return user.getFullName();
        else
            return "";
    }

    public String getGivename() {
        if (user != null)
            return user.getGivenName();
        else
            return "";
    }

    public String getVisibleName() {
        if (user == null)
            return "unknown";
        String name;
        name = user.getFullName();
        if (name != null && !name.isEmpty())
            return name;
        return user.getUsername();
    }

    public boolean hasRole(String rolename) {
	FacesContext fc = FacesContext.getCurrentInstance();

        if (user == null) {
            logger.info(String.format("There's no current user => no %s role", rolename));
            navigationBean.toLoginPage();
        }
        else if (!roles.hasUserRole(user, rolename)) {
            logger.info(String.format("User %s doesn't have role %s",
            user.getUsername(), rolename));
            return false;
        }

        return true;
    }

    public void redirectToWelcomePage(ComponentSystemEvent event)
            throws PermissionDenied
    {
        if (isValid())
            navigationBean.toWelcomePage(user);
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

    public void setLocalUser(AbstractUser u) {
        user = u;
    }

    public void setAuthSettings(AuthSettings as) {
        authSettings = as;
    }

    public void setLocalUserFactory(LocalUserFactory luf) {
        localUserFactory = luf;
    }
}
