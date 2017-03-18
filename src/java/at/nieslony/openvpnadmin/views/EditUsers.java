/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.ConfigBuilder;
import at.nieslony.openvpnadmin.Role;
import at.nieslony.openvpnadmin.VpnUser;
import at.nieslony.openvpnadmin.beans.LdapSettings;
import at.nieslony.openvpnadmin.beans.LocalUsers;
import at.nieslony.openvpnadmin.beans.Roles;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
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

    @ManagedProperty(value = "#{localUsers}")
    LocalUsers localUsers;

    @ManagedProperty(value = "#{ldapSettings}")
    LdapSettings ldapSettings;

    @ManagedProperty(value = "#{roles}")
    Roles roles;
    public void setRoles(Roles rb) {
        roles = rb;
    }

    @ManagedProperty(value ="#{configBuilder}")
    ConfigBuilder configBuilder;

    private String addLocalUserUsername;
    private String addLocalUserPassword;
    private String passwordResetUserName;
    private String passwordReset;
    private VpnUser selectedUser;

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


    public void setLocalUsers(LocalUsers lu) {
        localUsers = lu;
    }

    public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    public String getPasswordResetUsername() {
        return passwordResetUserName;
    }

    public VpnUser getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(VpnUser su) {
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

    public void onAddLocalUserOk() {
        localUsers.addUser(addLocalUserUsername, addLocalUserPassword);

        RequestContext.getCurrentInstance().execute("PF('dlgAddLocalUser').hide();");
    }

    public List<VpnUser> getAllUsers() {
        List<VpnUser> lu = localUsers.getUsers();
        lu.addAll(ldapSettings.findVpnUsers("*"));

        return lu;
    }

    public void onResetPassword(VpnUser user) {
        logger.info("Open dialog dlgResetPassword");
        passwordResetUserName = user.getUsername();
        RequestContext.getCurrentInstance().execute("PF('dlgResetPassword').show();");
    }

    public void onResetPasswordOk() {
        VpnUser user = localUsers.getUser(passwordResetUserName);
        user.setPasswordHash(localUsers.createSaltedHash(passwordReset));
        localUsers.saveUsers();
        RequestContext.getCurrentInstance().execute("PF('dlgResetPassword').hide();");
    }

    public void onRemoveUser(String username) {
        localUsers.removeUser(username);
        localUsers.saveUsers();
    }

    public StreamedContent getDownloadNetworkManagerInstaller() {
        StreamedContent sc = null;
        try {
            sc = configBuilder.getDownloadNetworkManagerConfig(selectedUser.getUsername());
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
        StreamedContent sc = null;
        try {
            sc = configBuilder.getDownloadOpenVpnConfig(selectedUser.getUsername());
        }
        catch (FileNotFoundException ex) {
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Not found",
                    "Cannot find user vpn  config"));
        }
        catch (IOException | CertificateEncodingException ex) {
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error",
                    "An inter error occurred. Cannot provide config. See logs"));
            logger.severe(String.format("Cannot provide openVPN config: %s", ex.getMessage()));
        }

        return sc;
    }

    public String getRolesString(String username) {
        List<String> rs = new LinkedList<>();

        for (Role role : roles.getRoles()) {
            if (role.isAssumedByUser(username))
                rs.add(role.getName());
        }

        if (rs.isEmpty())
            return "no access";
        else
            return String.join(", ", rs);
    }
}
