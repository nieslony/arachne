/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.naming.NamingException;
import org.primefaces.PrimeFaces;

/**
 *
 * @author claas
 */
@ManagedBean
@RequestScoped
public class LoginBean implements Serializable {
    private static final long serialVersionUID = 1234L;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private String username;
    private String password;

    @ManagedProperty(value="#{navigationBean}")
    private NavigationBean navigationBean;

    @ManagedProperty(value = "#{localUserFactory}")
    LocalUserFactory localUserFactory;

    @ManagedProperty(value = "#{databaseSettings}")
    DatabaseSettings databaseSettings;

    @ManagedProperty(value = "#{authSettings}")
    AuthSettings authSettings;

    @ManagedProperty(value = "#{ldapSettings}")
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
                catch (NamingException | NoSuchLdapUser ex) {
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
	FacesContext fc = FacesContext.getCurrentInstance();
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
