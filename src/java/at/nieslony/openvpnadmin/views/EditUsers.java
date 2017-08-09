/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.ConfigBuilder;
import at.nieslony.openvpnadmin.Role;
import at.nieslony.openvpnadmin.beans.LdapSettings;
import at.nieslony.openvpnadmin.beans.LocalUserFactory;
import at.nieslony.openvpnadmin.beans.Roles;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.bouncycastle.operator.OperatorCreationException;
import org.primefaces.context.RequestContext;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class EditUsers implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{ldapSettings}")
    LdapSettings ldapSettings;

    @ManagedProperty(value = "#{roles}")
    Roles roles;
    public void setRoles(Roles rb) {
        roles = rb;
    }

    @ManagedProperty(value = "#{localUserFactory}")
    LocalUserFactory localUserFactory;
    public void setLocalUserFactory(LocalUserFactory luf) {
        localUserFactory = luf;
    }

    @ManagedProperty(value ="#{configBuilder}")
    ConfigBuilder configBuilder;

    private String addLocalUserUsername;
    private String addLocalUserPassword;
    private String passwordResetUserName;
    private String passwordReset;
    private AbstractUser selectedUser;

    public void setConfigBuilder(ConfigBuilder cb) {
        configBuilder = cb;
    }

    public String getPasswordReset() {
        return passwordReset;
    }

    public void setPasswordReset(String passwordReset) {
        this.passwordReset = passwordReset;
    }

    public String getAddLocalUserPassword() {
        return addLocalUserPassword;
    }

    public void setAddLocalUserPassword(String addLocalUserPassword) {
        this.addLocalUserPassword = addLocalUserPassword;
    }

    public String getAddLocalUserUsername() {
        return addLocalUserUsername;
    }

    public void setAddLocalUserUsername(String addLocalUserUsername) {
        this.addLocalUserUsername = addLocalUserUsername;
    }

    public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    public String getPasswordResetUsername() {
        return passwordResetUserName;
    }

    public AbstractUser getSelectedUser() {
        logger.info(selectedUser == null ? "null" : selectedUser.getUsername());
        return selectedUser;
    }

    public void setSelectedUser(AbstractUser su) {
        selectedUser = su;
    }

    /**
     * Creates a new instance of EditUsersBean
     */
    public EditUsers() {
    }

    public void onAddLocalUser() {
        logger.info("Open dialog dlgAddLocalUser");
        RequestContext.getCurrentInstance().execute("PF('dlgAddLocalUser').show();");
    }

    public void onAddLocalUserOk()
    {
        AbstractUser user = localUserFactory.addUser(addLocalUserUsername);
        user.setPassword(addLocalUserPassword);
        try {
            user.save();
        }
        catch (Exception ex) {
            logger.warning(String.format("Cannot save user %s: %s",
                    passwordResetUserName, ex.getMessage()));
        }

        RequestContext.getCurrentInstance().execute("PF('dlgAddLocalUser').hide();");
    }

    public List<AbstractUser> getAllUsers() {
        List<AbstractUser> lu = new LinkedList<>();

        try {
            lu.addAll(localUserFactory.getAllUsers());
        }
        catch (ClassNotFoundException | SQLException ex) {
            String msg = String.format("Cannot fetch local users: %s", ex.getMessage());

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
            logger.warning(msg);
        }
        lu.addAll(ldapSettings.findVpnUsers("*"));

        return lu;
    }

    public void onResetPassword(AbstractUser user) {
        logger.info("Open dialog dlgResetPassword");
        passwordResetUserName = user.getUsername();
        RequestContext.getCurrentInstance().execute("PF('dlgResetPassword').show();");
    }

    public void onResetPasswordOk() {
        AbstractUser user = localUserFactory.findUser(passwordResetUserName);
        user.setPassword(passwordReset);
        try {
            user.save();
        }
        catch (Exception ex) {
            logger.warning(String.format("Cannot save user %s: %s",
                    passwordResetUserName, ex.getMessage()));
        }
        RequestContext.getCurrentInstance().execute("PF('dlgResetPassword').hide();");
    }

    public void onRemoveUser(String username) {
        try {
            if (!localUserFactory.removeUser(username)) {
                FacesContext.getCurrentInstance().addMessage(
                    null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO, "Warning", "No user removed"));
            }
            else {
                FacesContext.getCurrentInstance().addMessage(
                    null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO, "Warning",
                            String.format("User %s removed.", username)));
            }
        }
        catch (Exception ex) {
                FacesContext.getCurrentInstance().addMessage(
                    null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO, "Warning",
                            String.format("Cannot remove user %s: %s",
                                    username, ex.getMessage())));
        }
    }

    public StreamedContent getDownloadNetworkManagerInstaller(AbstractUser user)
            throws AbstractMethodError, ClassNotFoundException, GeneralSecurityException,
            OperatorCreationException, SQLException
    {
        logger.info(String.format("Preparing NetworkManager config of user %s fow download.",
                user.getUsername()));
        StreamedContent sc = null;
        try {
            sc = configBuilder.getDownloadNetworkManagerConfig(user.getUsername());
        }
        catch (FileNotFoundException ex) {
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Not found",
                    "Cannot find user vpn config"));
        }
        catch (IOException | CertificateEncodingException ex) {
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error",
                    "An inter error occurred. Cannot provide config. See logs"));
            logger.severe(String.format("Cannot provide NetworkManager installer: %s", ex.getMessage()));
        }

        return sc;
    }

    public StreamedContent getDownloadOpenVPNConfig() {
        logger.info(String.format("Preparing openVPN config of user %s fow download.",
                selectedUser));
        StreamedContent sc = null;
        try {
            sc = configBuilder.getDownloadOpenVpnConfig("claas" /*user.getUsername()*/);
        }
        catch (FileNotFoundException ex) {
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Not found",
                    "Cannot find user vpn  config"));
        }
        catch (IOException | CertificateEncodingException | AbstractMethodError
                | OperatorCreationException ex) {
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error",
                    "An inter error occurred. Cannot provide config. See logs"));
            logger.severe(String.format("Cannot provide openVPN config: %s", ex.getMessage()));
        }

        return sc;
    }

    public String getRolesString(AbstractUser user) {
        List<String> rs = new LinkedList<>();

        logger.info(String.format("Getting roles for user %s", user.getUsername()));

        for (Role role : roles.getRoles()) {
            if (role.isAssumedByUser(user))
                rs.add(role.getName());
        }

        if (rs.isEmpty())
            return "no access";
        else
            return String.join(", ", rs);
    }
}
