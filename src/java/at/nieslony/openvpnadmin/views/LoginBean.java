/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.beans.CurrentUser;
import at.nieslony.openvpnadmin.beans.DatabaseSettings;
import at.nieslony.openvpnadmin.beans.LocalUserFactory;
import at.nieslony.openvpnadmin.beans.NavigationBean;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import org.primefaces.context.RequestContext;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class LoginBean implements Serializable {
    private static final long serialVersionUID = 1234L;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private String username;
    private String password;

    @ManagedProperty(value="#{navigationBean}")
    private NavigationBean navigationBean;

    @ManagedProperty(value = "#{currentUser}")
    private CurrentUser currentUser;

    @ManagedProperty(value = "#{localUserFactory}")
    LocalUserFactory localUserFactory;

    @ManagedProperty(value = "#{databaseSettings}")
    DatabaseSettings databaseSettings;

    public void onLogin() throws PermissionDenied{
        AbstractUser tmpUser = localUserFactory.findUser(username);
        if (tmpUser != null && tmpUser.auth(password)) {
            currentUser.setLocalUser(tmpUser);
            logger.info(String.format("Navigating to %s's welcome page", username));
            navigationBean.toWelcomePage(tmpUser);
        }

        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
            "Login incorrect",
            "The username/password combination you entered is invalid. Please try again."
        );
        RequestContext.getCurrentInstance().showMessageInDialog(msg);
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

    public void setCurrentUser(CurrentUser cu) {
        currentUser = cu;
    }

    public void alreadyLoggedIn(ComponentSystemEvent event) {

    }

    public void requireSetup(ComponentSystemEvent event) throws IOException {
	FacesContext fc = FacesContext.getCurrentInstance();
        if (!databaseSettings.isValid() /*|| !pki.isValid() */ ) {
            /*ConfigurableNavigationHandler nav =
                    (ConfigurableNavigationHandler)
			fc.getApplication().getNavigationHandler();
*/
            logger.info("Navigating to SetupWizard");
            //nav.performNavigation("SetupWizard");
            fc.getExternalContext().redirect("SetupWizard.xhtml");
        }
    }


    public void setLocalUserFactory(LocalUserFactory luf) {
        localUserFactory = luf;
    }

    public void setDatabaseSettings(DatabaseSettings dbs) {
        databaseSettings = dbs;
    }
}
