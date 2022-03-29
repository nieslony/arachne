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

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.LdapUser;
import at.nieslony.openvpnadmin.beans.AuthSettings;
import at.nieslony.openvpnadmin.beans.CurrentUser;
import at.nieslony.openvpnadmin.beans.DatabaseSettings;
import at.nieslony.openvpnadmin.beans.LdapSettings;
import at.nieslony.openvpnadmin.beans.LocalUserFactory;
import at.nieslony.openvpnadmin.beans.NavigationBean;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapUser;
import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.ConfigurableNavigationHandler;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
import org.primefaces.PrimeFaces;

/**
 *
 * @author claas
 */
@RequestScoped
@Named
public class LoginBean implements Serializable {
    private static final long serialVersionUID = 1234L;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private String username;
    private String password;

    @Inject
    private NavigationBean navigationBean;

    @Inject
    LocalUserFactory localUserFactory;

    @Inject
    DatabaseSettings databaseSettings;

    @Inject
    AuthSettings authSettings;

    @Inject
    LdapSettings ldapSettings;

    public void onLogin() throws PermissionDenied{
        FacesContext ctx = FacesContext.getCurrentInstance();
        CurrentUser currentUser = ctx.getApplication()
                .evaluateExpressionGet(ctx, "#{currentUser}", CurrentUser.class);

        AbstractUser tmpUser = localUserFactory.findUser(username);
        if (tmpUser != null && tmpUser.auth(password)) {
            currentUser.setLocalUser(tmpUser);
            logger.info(String.format("Navigating to %s's welcome page", username));
            navigationBean.toWelcomePage(tmpUser);
        }
        else {
            if (authSettings.getAllowBasicAuthLdap()) {
                LdapUser ldapUser = null;

                try {
                    ldapUser = ldapSettings.findVpnUser(username);
                    if (ldapUser != null && ldapUser.auth(password)) {
                        currentUser.setLocalUser(ldapUser);
                        logger.info(String.format("Navigating to %s's welcome page", username));
                        navigationBean.toWelcomePage(ldapUser);
                    }
                }
                catch (NamingException | NoSuchLdapUser | LoginException ex) {
                    logger.warning(String.format("Cannot find LDAP user %s: %s",
                            username, ex.getMessage()));
                }
            }
        }

        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
            "Login incorrect",
            "The username/password combination you entered is invalid. Please try again."
        );
        PrimeFaces.current().dialog().showMessageDynamic(msg);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setNavigationBean(NavigationBean navigationBean) {
        this.navigationBean = navigationBean;
    }

    public void setPki(Pki pki) {
        //this.pki = pki;
    }

    public void alreadyLoggedIn(ComponentSystemEvent event) {

    }

    public void requireSetup(ComponentSystemEvent event) throws IOException {
	FacesContext fc = FacesContext.getCurrentInstance().getCurrentInstance();
        if (!databaseSettings.isValid() /*|| !pki.isValid() */ ) {
            ConfigurableNavigationHandler nav =
                    (ConfigurableNavigationHandler)
			fc.getApplication().getNavigationHandler();

            logger.info("Navigating to SetupWizard");
            nav.performNavigation("SetupWizard");
            //fc.getExternalContext().redirect("SetupWizard.xhtml");
        }
    }


    public void setLocalUserFactory(LocalUserFactory luf) {
        localUserFactory = luf;
    }

    public void setDatabaseSettings(DatabaseSettings dbs) {
        databaseSettings = dbs;
    }

    public void setAuthSettings(AuthSettings as) {
        authSettings = as;
    }

    public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }
}
