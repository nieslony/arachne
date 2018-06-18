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
import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class NavigationBean implements Serializable {
    private static final long serialVersionUID = 123L;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{roles}")
    private Roles roles;

    public void setRoles(Roles r) {
        roles = r;
    }

    public void toPage(String page) {
        logger.info(String.format("Forwarding to page %s", page));
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            logger.severe("There's no facesContext");
            return;
        }
        ExternalContext ec = fc.getExternalContext();
        if (ec == null) {
            logger.severe("There's no externalContext");
            return;
        }

        try {
            ec.redirect(page);
        }
        catch (IllegalStateException ex) {
            logger.info(String.format("Redirect doen't work, trying request dispatcher: %s", ex.getMessage()));
            HttpServletResponse resp = (HttpServletResponse) ec.getResponse();
            if (resp == null) {
                logger.severe("There's no response");
                return;
            }

            HttpServletRequest req = (HttpServletRequest) ec.getRequest();
            if (req == null) {
                logger.severe("There's no request");
                return;
            }

            try {
                RequestDispatcher rd = req.getRequestDispatcher("Login.xhtml");
                rd.forward(req, resp);
            } catch (IOException | ServletException | IllegalStateException ex2) {
                logger.severe(String.format("Cannot forward with request dispatcher to %s: %s",
                        page, ex.getMessage()));
            }

        }
        catch (Exception ex) {
            logger.severe(String.format("Cannot forward to %s: %s",
                    page, ex.getMessage()));
        }
    }

    public void toLoginPage() {
        toPage("Login.xhtml");
    }

    public void toWelcomePage() {

    }

    public void toWelcomePage(AbstractUser user)
            throws PermissionDenied
    {
        if (user == null) {
            logger.info("There's no current user, forwarding to login page");
            toLoginPage();
            return;
        }

        if (roles.hasUserRole(user, "admin")) {
            logger.info(String.format("User %s has role admin => redirect to AdminWelcome",
                    user.getUsername()));
            toPage("AdminWelcome.xhtml");
            return;
        }
        if (roles.hasUserRole(user, "user")) {
            logger.info(String.format("User %s has role user=> redirect to UserWelcome",
                    user.getUsername()));
            toPage("UserWelcome.xhtml");
            return;
        }
        throw new PermissionDenied("User " + user.getUsername() + " has neither role admin nor role user");
    }
}
