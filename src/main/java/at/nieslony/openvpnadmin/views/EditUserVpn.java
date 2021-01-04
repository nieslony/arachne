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
import at.nieslony.openvpnadmin.beans.FolderFactory;
import at.nieslony.openvpnadmin.beans.ManagementInterface;
import at.nieslony.openvpnadmin.beans.UserVpn;
import at.nieslony.openvpnadmin.beans.base.UserVpnBase;
import at.nieslony.openvpnadmin.exceptions.ManagementInterfaceException;
import at.nieslony.openvpnadmin.views.base.EditUserVpnBase;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.PrimeFaces;

@ViewScoped
@Named
public class EditUserVpn
    extends EditUserVpnBase
    implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private final List<String> pushRoutes = new ArrayList<>();
    private final List<String> dnsServers = new ArrayList<>();
    private String selDnsServer;
    private String editDnsServer;
    private String selPushRoute;
    private String editPushRouteNetwork;
    private String editPushRouteMask;
    private String authWebserverProtocol = "http";
    private String authWebserverHostPath = "localhost";

    public EditUserVpn () {
    }

    @Inject
    UserVpn userVpn;

    @Inject
    ConfigBuilder configBuilder;

    @Inject
    FolderFactory folderFactory;

    @Inject
    private AdminWelcome adminWelcome;

    @Inject
    private ManagementInterface managementInterface;

    @PostConstruct
    public void init() {
        setBackend(userVpn);
        load();
        URL url;
        try {
            url = new URL(getAuthScriptUrl());
        }
        catch (MalformedURLException ex) {
            url = null;
        }
        if (url == null) {
            authWebserverProtocol = "http";
            authWebserverHostPath = "localhost/arachne";
        }
        else {
            authWebserverProtocol = url.getProtocol();
            StringBuilder sb = new StringBuilder();
            sb.append(url.getHost());
            if (
                    (url.getProtocol().equals("http") && url.getPort() != 80 && url.getPort() != -1)
                    |
                    (url.getProtocol().equals("https") && url.getPort() != 443 && url.getPort() != -1)
                    ) {
                sb.append(":").append(url.getPort());
            }
            sb.append(url.getPath());
            authWebserverHostPath = sb.toString();
        }

        updatePushRoutesList();
        updateDnsServersList();
    }

    public void onSave()
            throws IOException
    {
        joinDnsServers();
        joinPushRoutes();
        setIsEnabled(true);

        StringBuilder sb = new StringBuilder();
        sb.append(authWebserverProtocol)
                .append("://")
                .append(authWebserverHostPath);
        setAuthScriptUrl(sb.toString());
        save();

        String iniFile = folderFactory.getUserVpnIniFileName();
        logger.info(String.format("Writing server ini file to %s", iniFile));
        try (FileWriter fwr = new FileWriter(iniFile)) {
            configBuilder.writeUserVpnServerIni(fwr);
        }
        catch (IOException ex) {
            logger.warning(String.format("Cannot write server ini file: %s", ex.getMessage()));
        }

        String serverConfigFile = folderFactory.getUserVpnFileName();
        logger.info(String.format("Writing server config to %s", serverConfigFile));
        FileWriter fwr = null;
        try {
            fwr = new FileWriter(serverConfigFile);
            configBuilder.writeUserVpnServerConfig(fwr);
            fwr.flush();
            fwr.close();
        }
        catch (CertificateEncodingException | IOException ex) {
            logger.warning(String.format("Cannot write server config: %s", ex.getMessage()));
        }
        finally {
            if (fwr != null)
                fwr.close();
        }

        FacesContext.getCurrentInstance().addMessage(
                null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO, "Info", "Settings saved."));

        adminWelcome.loadUserVpns();
        try {
            managementInterface.sendSignal(ManagementInterface.Signal.Hup);
        }
        catch (IOException | ManagementInterfaceException ex) {
            logger.warning(String.format("Cannot trigger reload of VPN configuration: %s",
                    ex.getMessage()));
        }

        PrimeFaces.current().ajax().update("menuForm:mainMenu");
    }

    private void updatePushRoutesList() {
        pushRoutes.clear();
        String[] routes = getPushRoutes().split("\\s*,\\s*");
        pushRoutes.addAll(Arrays.asList(routes));
    }

    private void updateDnsServersList() {
        dnsServers.clear();
        String[] ds = getDnsServers().split("\\s*,\\s*");
        dnsServers.addAll(Arrays.asList(ds));
    }

    private void joinPushRoutes() {
        setPushRoutes(String.join(",", pushRoutes));
    }

    private void joinDnsServers() {
        setDnsServers(String.join(",", dnsServers));
    }

    public void onReset() {
        load();
        updatePushRoutesList();
        updateDnsServersList();
    }

    public void onResetToDefaults() {
        resetDefaults();
        updatePushRoutesList();
        updateDnsServersList();
    }

    public void setUserVpn(UserVpn v) {
        userVpn = v;
    }

    public void setConfigBuilder(ConfigBuilder cb) {
        configBuilder = cb;
    }

    public void setFolderFactory(FolderFactory ff) {
        folderFactory = ff;
    }

    public void setAdminWelcome(AdminWelcome ab) {
        this.adminWelcome = ab;
    }

    public void setManagementInterface(ManagementInterface mi) {
        managementInterface = mi;
    }

    public List<String> getDnsServersList() {
        return dnsServers;
    }

    public List<String> getPushRoutesList() {
        return pushRoutes;
    }

    public String getSelDnsServer() {
        return selDnsServer;
    }

    public void setSelDnsServer(String selDnsServer) {
        this.selDnsServer = selDnsServer;
    }

    public String getEditDnsServer() {
        return selDnsServer;
    }

    public void setEditDnsServer(String editDnsServer) {
        this.editDnsServer = editDnsServer;
    }

    public void setSelPushRoute(String s) {
        this.selPushRoute = s;
    }

    public String getSelPushRoute() {
        return selPushRoute;
    }

    public void setEditPushRouteNetwork(String s) {
        this.editPushRouteNetwork = s;
    }

    public String getEditPushRouteNetwork() {
        if (selPushRoute == null)
            return "";

        String[] sa = selPushRoute.split("/");

        if (sa.length == 2)
            return sa[0];
        else
            return "???";
    }

    public void setEditPushRouteMask(String s) {
        this.editPushRouteMask = s;
    }

    public String getEditPushRouteMask() {
        if (selPushRoute == null)
            return "";

        String[] sa = selPushRoute.split("/");

        if (sa.length == 2)
            return sa[1];
        else
            return "???";
    }

    public void setAuthWebserverHostPath(String hp) {
        authWebserverHostPath = hp;
    }

    public String getAuthWebserverHostPath() {
        return authWebserverHostPath;
    }

    public void onAddRoute() {
        String routeStr = editPushRouteNetwork + "/" + editPushRouteMask;
        logger.info(String.format("Adding route %s", routeStr));
        pushRoutes.add(routeStr);
        selPushRoute = routeStr;
    }

    public void onModifyRoute() {
        String routeStr = editPushRouteNetwork + "/" + editPushRouteMask;

        int index = pushRoutes.indexOf(selPushRoute);
        if (index != -1) {
            pushRoutes.set(index, routeStr);
        }
    }

    public void onRemoveRoute() {
        int index = pushRoutes.indexOf(selPushRoute);
        if (index != -1) {
            pushRoutes.remove(index);
        }
    }

    public void onAddDnsServer() {
        logger.info(String.format("Adding DNS server %s", editDnsServer));
        dnsServers.add(editDnsServer);
    }

    public void onModifyDnsServer() {
        logger.info(String.format("Replacing DNS server %s by %s", selDnsServer, editDnsServer));
        int index = dnsServers.indexOf(selDnsServer);
        if (index != -1) {
            dnsServers.set(index, editDnsServer);
        }
        else {
            logger.warning(String.format("Cannot find index of %s", selDnsServer));
        }
    }

    public void onRemoveDnsServer() {
        logger.info(String.format("Removong DNS server %s", selDnsServer));
        int index = dnsServers.indexOf(selDnsServer);
        if (index != -1) {
            dnsServers.remove(index);
        }
        else {
            logger.warning(String.format("Cannot find index of %s", selDnsServer));
        }
    }

    public Boolean getRenderPamService() {
        return getUserPasswordMode() == UserVpnBase.UserPasswordMode.PAM &&
                getAuthType() != UserVpnBase.VpnAuthType.CERTIFICATE;
    }

    public Boolean getRenderSslSettings() {
        return getUserPasswordMode() == UserVpnBase.UserPasswordMode.HTTP &&
                getAuthType() != UserVpnBase.VpnAuthType.CERTIFICATE &&
                authWebserverProtocol.equals("https");
    }

    public Boolean getRenderAuthUrl() {
        return getUserPasswordMode() == UserVpnBase.UserPasswordMode.HTTP &&
                getAuthType() != UserVpnBase.VpnAuthType.CERTIFICATE;
    }

    public Boolean getRenderWebserverCaFile() {
        return getRenderSslSettings() && !getAuthCaDefault();
    }

    public Boolean getRenderUserPasswordMode() {
        return getAuthType() != UserVpnBase.VpnAuthType.CERTIFICATE;
    }

    public UserVpnBase.VpnAuthType[] getAuthTypes() {
        return UserVpnBase.VpnAuthType.values();
    }

    public UserVpnBase.UserPasswordMode[] getUserPasswordModes() {

        return UserVpnBase.UserPasswordMode.values();
    }

    public String getAuthWebserverProtocol() {
        return authWebserverProtocol;
    }

    public void setAuthWebserverProtocol(String protocol) {
        authWebserverProtocol = protocol;
    }

    public void onRemove() {
        setIsEnabled(false);
        save();
        adminWelcome.loadUserVpns();
        PrimeFaces.current().ajax().update("menuForm:mainMenu");

        String serverConfigFile = folderFactory.getUserVpnFileName();
        logger.info(String.format("Removing %s", serverConfigFile));
        File f = new File(serverConfigFile);
        f.delete();
    }
}
