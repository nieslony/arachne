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

import at.nieslony.openvpnadmin.ConfigBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import org.bouncycastle.operator.OperatorCreationException;

/**
 *
 * @author claas
 */
@ManagedBean
@RequestScoped
public class DownloadClientConfig
        implements Serializable
{
    private static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{currentUser}")
    CurrentUser currentUser;

    public void setCurrentUser(CurrentUser cub) {
        currentUser = cub;
    }

    @ManagedProperty(value = "#{configBuilder}")
    ConfigBuilder configBuilder;

    public void setConfigBuilder(ConfigBuilder cb) {
        configBuilder = cb;
    }

    /**
     * Creates a new instance of DownloadClientConfig
     */
    public DownloadClientConfig() {
    }

    public void getOvpnConfig(ComponentSystemEvent event)
            throws OperatorCreationException
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        Writer wr = null;
        try {
            wr = ec.getResponseOutputWriter();
            ec.setResponseContentType("text/plain");
            ec.setResponseCharacterEncoding("UTF-8");
        }
        catch (IOException ex) {
            logger.warning(String.format("Cannot get response writer: %s", ex.getMessage()));
            return;
        }

        String username = currentUser.getUsername();
        try {
            configBuilder.writeUserVpnClientConfig(wr, username);
        }
        catch (CertificateEncodingException | IOException ex) {
            logger.warning(String.format("Error getting openvpn configuration for user %s: %s",
                    username, ex.getMessage()));

            PrintWriter pw = new PrintWriter(wr);
            pw.println("# Error getting configuration.");
        }
    }

    public void getNetworkManagerConfig(ComponentSystemEvent event)
            throws AbstractMethodError, OperatorCreationException
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        Writer wr = null;
        try {
            wr = ec.getResponseOutputWriter();
            ec.setResponseContentType("text/plain");
            ec.setResponseCharacterEncoding("UTF-8");
        }
        catch (IOException ex) {
            logger.warning(String.format("Cannot get response writer: %s", ex.getMessage()));
            return;
        }

        String username = currentUser.getUsername();
        try {
            configBuilder.writeUserVpnNetworkManagerConfig(wr, username);
        }
        catch (ClassNotFoundException | GeneralSecurityException | IOException | SQLException ex) {
            logger.warning(String.format("Error getting network manager configuration for user %s: %s",
                    username, ex.getMessage()));

            PrintWriter pw = new PrintWriter(wr);
            pw.println("# Error getting configuration.");
        }
    }

}
