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
import java.io.IOException;
import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
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

    transient private Map<String, Boolean> cachedRoles = new HashMap<>();

    /**
     * Creates a new instance of CurrentUserBean
     */
    public CurrentUser() {
    }

    private void initWithAjpRemoteUser(HttpServletRequest req) {
        try {
            if (authSettings.getEnableAjpRemoteUser()) {
                logger.info("AJP remoteUser enabled");
                if (req.getRemoteUser() != null) {
                    try {
                        user = ldapSettings.findVpnUser(req.getRemoteUser());
                    }
                    catch (NamingException | NoSuchLdapUser ex) {
                        logger.info(String.format("Cannot find LDAP user %s: %s",
                                req.getRemoteUser(), ex.getMessage()));
                    }
                }
                else {
                    logger.info("No remoteUser supplied");
                }
            }
            else {
                logger.info("AJP remoteUser disabled");
            }
        }
        catch (Exception ex) {
            logger.warning(String.format("Cannot initialize AJP remote user: %s",
                    ex.getMessage()));
        }
    }

    private void initWithBasicAuth(HttpServletRequest req)
            throws InvalidUsernameOrPassword
    {
        if (req.getHeader("authorization") != null) {
            String auth[] = req.getHeader("authorization").split(" ");
            if (auth.length == 2 && auth[0].equals("Basic")) {
                byte[] decoded = Base64.getDecoder().decode(auth[1]);
                String[] usrPwd = new String(decoded).split(":");
                if (usrPwd.length == 2) {
                    String username = usrPwd[0];
                    String password = usrPwd[1];

                    logger.info(String.format("Trying basic auth with local user %s...",
                            username));
                    AbstractUser tmpUser;

                    tmpUser = localUserFactory.findUser(username);
                    if (tmpUser != null) {
                        if (tmpUser.auth(password)) {
                            user = tmpUser;
                        }
                        else {
                            throw new InvalidUsernameOrPassword();
                        }
                    }
                    else
                        if (authSettings.getAllowBasicAuthLdap()) {
                        logger.info(String.format("Trying basic auth with LDAP user %s...",
                                username));
                        try {
                            tmpUser = ldapSettings.findVpnUser(username);
                            if (tmpUser != null) {
                                if (tmpUser.auth(password)) {
                                    user = tmpUser;
                                }
                                else {
                                    throw new InvalidUsernameOrPassword();
                                }
                            }
                        }
                        catch (NamingException | NoSuchLdapUser ex) {
                            logger.warning(String.format("Cannot find LDAP user %s: %s",
                                    username, ex.getMessage()));
                        }
                    }
                    else {
                        logger.info("Basic auth with LDAP disabled");
                    }
                }
            }
        }
    }

    @PostConstruct
    public void init()
    {
        logger.info("Initializing currentUser");

        ExternalContext ectx = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest req = (HttpServletRequest) ectx.getRequest();
        user = null;
        cachedRoles.clear();

        try {
            initWithAjpRemoteUser(req);
            if (user ==  null) {
                initWithBasicAuth(req);
            }
        }
        catch (InvalidUsernameOrPassword iuop) {
            logger.severe(String.format("Illegal REMOTE_USER or password provided: %s",
                    iuop.getMessage()));

            ectx.setResponseStatus(403);
            Map<String, Object> requestMap = ectx.getRequestMap();
            requestMap.put("errorMsg", "Permission denied");
            String errPage = "/error/error.xhtml";
            try {
                ectx.dispatch(errPage);
            }
            catch (IOException ex) {
                logger.severe(String.format("Cannot go to error page: %s",
                ex.getMessage()));
            }
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
        if (user == null) {
            logger.info(String.format("There's no current user => no %s role", rolename));
            navigationBean.toLoginPage();
        }
        if (rolename == null) {
            logger.info("There are no empty role names. Don't ask me!");
            return false;
        }
        Boolean hr = null;
        if (cachedRoles == null) {
            logger.severe("Role cache == null. Shoukd not occur");
            cachedRoles = new HashMap<>();
        }
        hr = cachedRoles.get(rolename);
        if (hr != null) {
            logger.info(String.format("Return from cache: user %s %s role %s",
                    user.getUsername(),
                    hr ? "has role" : "doesn't have role",
                    rolename));
            return hr;
        }
        if (!roles.hasUserRole(user, rolename)) {
            logger.info(String.format("User %s doesn't have role %s, add entry to cache",
                user.getUsername(), rolename));
            cachedRoles.put(rolename, Boolean.FALSE);
            return false;
        }
        else {
            logger.info(String.format("User %s has role %s, add entry to cache",
                user.getUsername(), rolename));
            cachedRoles.put(rolename, Boolean.TRUE);
            return true;
        }
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
