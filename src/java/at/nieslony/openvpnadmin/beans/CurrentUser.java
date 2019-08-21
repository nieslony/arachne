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
import javax.annotation.PreDestroy;
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
    /**
	 * 
	 */
	private static final long serialVersionUID = -7348758613584257146L;

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
        AbstractUser tmpUser = null;
        try {
            if (authSettings.getEnableAjpRemoteUser()) {
                logger.info("AJP remoteUser enabled");
                if (req.getRemoteUser() != null) {
                    try {
                        tmpUser = ldapSettings.findVpnUser(req.getRemoteUser());
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

        if (tmpUser != null) {
            logger.info(String.format("Found remote user %s", tmpUser.getUsername()));
            user = tmpUser;
        }
    }

    private void initWithBasicAuth(HttpServletRequest req)
            throws InvalidUsernameOrPassword
    {
        logger.info("Trying basic auth");
        logger.info(req.getAuthType());
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null) {
            logger.info("There's no authentication header");
            return;
        }

        String auth[] = authHeader.split(" ");
        if (auth.length != 2) {
            logger.severe("Cannot parse authentication header");
            throw new InvalidUsernameOrPassword();
        }
        if (!auth[0].equals("Basic")) {
            logger.info(String.format("Ignoring auth type %s", auth[0]));
            return;
        }

        byte[] decoded = Base64.getDecoder().decode(auth[1]);
        String[] usrPwd = new String(decoded).split(":");

        if (usrPwd.length != 2) {
            logger.severe("Cannot split username/password");
            throw new InvalidUsernameOrPassword();
        }

        String username = usrPwd[0];
        String password = usrPwd[1];

        logger.info(String.format("Trying basic auth with local user %s...",
                username));
        AbstractUser tmpUser;

        tmpUser = localUserFactory.findUser(username);
        if (tmpUser != null) {
            if (tmpUser.auth(password)) {
                user = tmpUser;
                logger.info("Basic auth with local user succeeded.");
                return;
            }

            logger.severe("Authentication faled");
            throw new InvalidUsernameOrPassword();
        }

        if (!authSettings.getAllowBasicAuthLdap()) {
            logger.info("Basic auth with LDAP disabled, basic auth failed");
            throw new InvalidUsernameOrPassword();
        }

        logger.info(String.format("Trying basic auth with LDAP user %s...", username));
        try {
            tmpUser = ldapSettings.findVpnUser(username);
            if (tmpUser != null) {
                if (tmpUser.auth(password)) {
                    user = tmpUser;
                    logger.info("Basic auth with LDAP succeeded.");
                    return;
                }
                logger.severe("Cannot validate password");
                throw new InvalidUsernameOrPassword();
            }
            else {
                logger.info(String.format("User %s not found in LDAP, authentication failed", username));
                throw new InvalidUsernameOrPassword();
            }
        }
        catch (NamingException | NoSuchLdapUser ex) {
            logger.warning(String.format("Cannot find LDAP user %s: %s",
                        username, ex.getMessage()));
            throw new InvalidUsernameOrPassword();
        }
    }

    @PreDestroy
    public void preDestroy() {
        user = null;
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
                logger.warning("Forwarting to error page");
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
            return false;
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

    public AbstractUser getUser() {
        return user;
    }
}
