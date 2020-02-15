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
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.bouncycastle.operator.OperatorCreationException;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author claas
 */
@SessionScoped
@Named
public class UserWelcome implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    CurrentUser currentUser;

    @Inject
    private FolderFactory folderFactory;

    @Inject
    private ConfigBuilder configBuilder;

    @Inject
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
