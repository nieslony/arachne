/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.ConfigBuilder;
import at.nieslony.openvpnadmin.beans.CurrentUser;
import at.nieslony.openvpnadmin.beans.FolderFactory;
import at.nieslony.openvpnadmin.beans.Pki;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author claas
 */
@ManagedBean
@SessionScoped
public class UserWelcome implements Serializable {

    @ManagedProperty(value = "#{currentUser}")
    CurrentUser currentUser;

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;

    @ManagedProperty(value = "#{configBuilder}")
    private ConfigBuilder configBuilder;

    @ManagedProperty(value = "#{pki}")
    Pki pki;

    public void setConfigBuilder(ConfigBuilder cb) {
        configBuilder = cb;
    }

    public void setCurrentUser(CurrentUser u) {
        currentUser = u;
    }

    public void setFolderFactory(FolderFactory fc) {
        this.folderFactory = fc;
    }

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    /**
     * Creates a new instance of UserWelcomeBean
     */
    public UserWelcome() {
    }

    public StreamedContent getDownloadOpenVpnConfig() throws IOException, CertificateEncodingException {
        return configBuilder.getDownloadOpenVpnConfig(currentUser.getUsername());
    }

    public StreamedContent getDownloadNetworkManagerConfig() throws IOException, CertificateEncodingException {
        return configBuilder.getDownloadNetworkManagerConfig(currentUser.getUsername());
    }
}
