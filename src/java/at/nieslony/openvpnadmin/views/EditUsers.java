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
import org.primefaces.PrimeFaces;
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
    private boolean addLocalUserHasRoleUser;
    private boolean addLocalUserHasRoleAdmin;

    private String editUserUsername;
    private String editUserFullName;
    private String editUserEmail;
    private AbstractUser editUser;

    private String passwordResetUserName;
    private String passwordReset;
    private AbstractUser selectedUser;

    public void setEditUserEmail(String email) {
        editUserEmail = email;
    }

    public String getEditUserEmail() {
        return editUserEmail;
    }

    public void setEditUserFullName(String fn) {
        editUserFullName = fn;
    }

    public String getEditUserFullName() {
        return editUserFullName;
    }

    public void setEditUserUsername(String un) {
        editUserUsername = un;
    }

    public String getEditUserUsername() {
        return editUserUsername;
    }

    public void setAddLocalUserHasRoleUser(boolean b) {
        addLocalUserHasRoleUser = b;
    }

    public boolean getAddLocalUserHasRoleUser() {
        return addLocalUserHasRoleUser;
    }

    public void setAddLocalUserHasRoleAdmin(boolean b) {
        addLocalUserHasRoleAdmin = b;
    }

    public boolean getAddLocalUserHasRoleAdmin() {
        return addLocalUserHasRoleAdmin;
    }

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

    public void onEditUser(AbstractUser user) {
        logger.info(String.format("Editing user %s", user.getUsername()));
        editUserUsername = user.getUsername();
        editUserFullName = user.getFullName();
        editUserEmail = user.getEmail();

        editUser = user;

        PrimeFaces.current().executeScript("PF('dlgEditUser').show();");
    }

    public void onEditUserOk() {
        logger.info(String.format("Writing properties for user %s", editUser.getUsername()));
        editUser.setEmail(editUserEmail);
        editUser.setFullName(editUserFullName);

        try {
            editUser.save();

            String msg = String.format("Uer %s updated", editUser.getUsername());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", msg));
            logger.info(msg);
        }
        catch (Exception ex) {
            String msg = String.format("Cannot save user %s: %s",
                    editUser.getUsername(), ex.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
            logger.warning(msg);
        }

        PrimeFaces.current().executeScript("PF('dlgEditUser').hide();");
    }

    public void onAddLocalUser() {
        logger.info("Open dialog dlgAddLocalUser");
        PrimeFaces.current().executeScript("PF('dlgAddLocalUser').show();");
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
        if (addLocalUserHasRoleAdmin) {
            roles.addRule("admin", "isUser", user.getUsername());
        }
        if (addLocalUserHasRoleUser) {
            roles.addRule("user", "isUser", user.getUsername());
        }

        PrimeFaces.current().executeScript("PF('dlgAddLocalUser').hide();");
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
        PrimeFaces.current().executeScript("PF('dlgResetPassword').show();");
    }

    public void onResetPasswordOk() {
        AbstractUser user = localUserFactory.findUser(passwordResetUserName);
        user.setPassword(passwordReset);
        try {
            user.save();
            String msg = String.format("Password for user %s resetted.", user.getUsername());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", msg));
            logger.info(msg);
        }
        catch (Exception ex) {
            String msg = String.format("Cannot save user %s: %s",
                    passwordResetUserName, ex.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
            logger.warning(msg);
        }
        PrimeFaces.current().executeScript("PF('dlgResetPassword').hide();");
    }

    public void onRemoveUser(String username) {
        logger.info(String.format("Removing local user %s..", username));
        try {
            if (!localUserFactory.removeUser(username)) {
                FacesContext.getCurrentInstance().addMessage(
                    null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN, "Warning",
                            String.format("User %s not removed", username)));
            }
            else {
                FacesContext.getCurrentInstance().addMessage(
                    null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO, "Info",
                            String.format("User %s removed.", username)));
            }
        }
        catch (Exception ex) {
                FacesContext.getCurrentInstance().addMessage(
                    null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, "Error",
                            String.format("Cannot remove user %s: %s",
                                    username, ex.getMessage())));
        }
        roles.removeRuleFromRole("admin", "isUser", username);
        roles.removeRuleFromRole("user", "isUser", username);
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
