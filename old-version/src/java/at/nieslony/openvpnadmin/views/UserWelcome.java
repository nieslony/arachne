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
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import org.bouncycastle.operator.OperatorCreationException;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author claas
 */
@ManagedBean
@SessionScoped
public class UserWelcome implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

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

    public StreamedContent getDownloadOpenVpnConfig()
            throws IOException, CertificateEncodingException
    {
        StreamedContent content = null;

        try {
            content = configBuilder.getDownloadOpenVpnConfig(currentUser.getUsername());
        }
        catch (AbstractMethodError | CertificateEncodingException |
                IOException | OperatorCreationException ex) {
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error",
                            "Cannot get configuration file:"
                            )
                    );
        }

        return content;
    }

    public StreamedContent getDownloadNetworkManagerConfig()
            throws IOException, CertificateEncodingException, ClassNotFoundException,
            GeneralSecurityException, SQLException
    {
        StreamedContent content = null;

        try {
            content = configBuilder.getDownloadNetworkManagerConfig(currentUser.getUsername());
        }
        catch (AbstractMethodError | CertificateEncodingException |
                IOException | OperatorCreationException ex) {
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error",
                            "Cannot get configuration file:"
                            )
                    );
        }

        return content;
    }
}
