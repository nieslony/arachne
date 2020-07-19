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

package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.FolderFactory;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.UserVpn;
import at.nieslony.openvpnadmin.beans.base.UserVpnBase;
import at.nieslony.utils.NetUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.bouncycastle.operator.OperatorCreationException;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author claas
 */
@ApplicationScoped
@Named
public class ConfigBuilder implements Serializable {
    private static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    private Pki pki;

    @Inject
    FolderFactory folderFactory;

    @Inject
    UserVpn userVpn;

    public void setUserVpn(UserVpn uv) {
        userVpn = uv;
    }

    public void setFolderFactory(FolderFactory ff) {
        this.folderFactory = ff;
    }

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    public ConfigBuilder()
    {
    }

    public void writeUserVpnClientConfig(Writer wr, String username)
        throws IOException, CertificateEncodingException, OperatorCreationException
    {
        PrintWriter pr = new PrintWriter(wr);

        pr.println("# openVPN config for user " + username);
        pr.println("client");
        pr.println("remote " + userVpn.getHost());
        pr.println("port " + userVpn.getPort());
        pr.println("dev " + userVpn.getDeviceType().name().toLowerCase());
        pr.println("proto " + userVpn.getProtocol().name().toLowerCase());

        if (userVpn.getAuthType() != UserVpnBase.VpnAuthType.CERTIFICATE) {
            pr.println("auth-user-pass");
        }

        pr.println();
        pr.println("<ca>");
        pki.writeCaCert(pr);
        pr.println("</ca>");

        if (userVpn.getAuthType() != UserVpnBase.VpnAuthType.USERPWD) {
            Pki.KeyAndCert kac = null;
            try {
                kac = pki.getUserKeyAndCert(username);
            }
            catch (ClassNotFoundException | GeneralSecurityException | IOException | SQLException ex) {
                logger.warning(String.format("Cannot get private key and certificate for user %s: %s",
                        username, ex.getMessage()));
            }

            if (kac != null) {
                pr.println("<cert>");
                pki.writeCertificate(kac.getCert(), pr);
                pr.println("</cert>");

                pr.println("<key>");
                pki.writePrivateKey(kac.getKey(), pr);
                pr.println("</key>");
            }
        }
    }

    public void writeUserVpnServerIni(Writer wr) {
        PrintWriter pr = new PrintWriter(wr);

        pr.println(String.format("%s=%s%s",
                "url",
                userVpn.getAuthScriptUrl(),
                folderFactory.getAuthPage()));
        pr.println(String.format("%s=%b", "ignoressl", userVpn.getIgnoreSslErrors()));
        if (!userVpn.getAuthCaDefault())
            pr.println(String.format("%s=%s", "cafile", userVpn.getAuthCaFile()));
        pr.println(String.format("%s=%b", "manageFirewall", userVpn.getManageFirewall()));
        if (userVpn.getManageFirewall()) {
            pr.println(String.format("%s=%s", "firewallZone", userVpn.getFirewallZone()));
        }

        pr.close();
    }

    public void writeUserVpnServerConfig(Writer wr)
            throws CertificateEncodingException, IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("plugin ")
                .append(folderFactory.getPluginDir())
                .append("/arachne.so")
                .append(" config=").append(folderFactory.getUserVpnIniFileName());
//                .append(" url=").append(userVpn.getAuthScriptUrl()).append("/AuthOpenVPN.xhtml");
        if (userVpn.getIgnoreSslErrors()) {
            sb.append(" ignoressl=true");
        }
        if (!userVpn.getAuthCaDefault()) {
            sb.append(" cafile=").append(userVpn.getAuthCaFile());
        }
        String authPlugin = sb.toString();
        PrintWriter pr = new PrintWriter(wr);

        pr.println("port " + userVpn.getPort());
        pr.println("proto " + userVpn.getProtocol().name().toLowerCase());
        pr.println("dev " + userVpn.getDeviceType().name().toLowerCase());
        pr.println("server " + userVpn.getClientNetwork() +
                " " + NetUtils.maskLen2Mask(userVpn.getClientNetmask())
        );
        pr.println("<ca>");
        pki.writeCaCert(pr);
        pr.println("</ca>");
        pr.println("<key>");
        pki.writePrivateKey(pki.getServerKey(), pr);
        pr.println("</key>");
        pr.println("<cert>");
        pki.writeCertificate(pki.getServerCert(), pr);
        pr.println("</cert>");
        pr.println("dh " + pki.getDhFilename());
        pr.println("crl-verify " + pki.getCrlFilename());
        pr.println("keepalive " + userVpn.getPing() + " " + userVpn.getPingRestart());
        pr.println("management 127.0.0.1 9544");
        switch (userVpn.getAuthType()) {
            case USERPWD_CERTIFICATE:
                pr.println(authPlugin);
                break;
            case USERPWD:
                pr.println(authPlugin);
                pr.println("client-cert-not-required");
                pr.println("username-as-common-name");
        }
        String dnsServers = userVpn.getDnsServers();
        if (dnsServers != null && !dnsServers.isEmpty()) {
            for (String dns : dnsServers.split(",")) {
                if (!dns.isEmpty())
                    pr.println("push \"dhcp-option DNS " + dns + "\"");
            }
        }
        String routes = userVpn.getPushRoutes();
        if (routes != null && !routes.isEmpty()) {
            for (String route : routes.split(",")) {
                if (!route.isEmpty()) {
                    String[] split = route.split("/");
                    String ip = split[0];
                    int prefix = Integer.parseInt(split[1]);
                    String mask = NetUtils.maskLen2Mask(prefix);
                    pr.println("push \"route " + ip + " " + mask + "\"");
                }
            }
        }
    }

    public void writeUserVpnNetworkManagerConfig(Writer wr, String username)
        throws ClassNotFoundException, GeneralSecurityException, IOException,
            OperatorCreationException, AbstractMethodError, SQLException
    {
        boolean writeUserCert = userVpn.getAuthType() != UserVpnBase.VpnAuthType.USERPWD.USERPWD;

        PrintWriter pr = new PrintWriter(wr);

        String vpnName = userVpn.getNmConnectionTemplate();
        if (vpnName != null && vpnName.length() > 0)
            vpnName = vpnName
                    .replaceAll("%u", username)
                    .replaceAll("%h", userVpn.getHost())
                    .replaceAll("%n", userVpn.getConnectionName());
        else
            vpnName = "Unnamed VPN connection";

        StringBuilder vpnOpts = new StringBuilder();
        vpnOpts.append("remote = $VPN_HOST");
        vpnOpts.append(", port = $VPN_PORT");
        vpnOpts.append(", ca = $VPN_CA");
        vpnOpts.append(", password-flags = 1");

        switch (userVpn.getAuthType()) {
            case USERPWD_CERTIFICATE:
                vpnOpts.append(", connection-type = password-tls")
                        .append(", cert-pass-flags = 4");
                break;
            case CERTIFICATE:
                vpnOpts.append(", connection-type = tls");
                break;
            case USERPWD:
                vpnOpts.append(", connection-type = password");
                break;
        }

        if (userVpn.getAuthType() != UserVpnBase.VpnAuthType.USERPWD) {
            vpnOpts.append(", cert = $VPN_USER_CERT");
            vpnOpts.append(", key = $VPN_USER_KEY");
        }

        vpnOpts.append(", username = $VPN_USER");
        if (userVpn.getProtocol() == UserVpnBase.Protocol.TCP)
            vpnOpts.append(", proto-tcp = yes");

        pr.println("#!/bin/bash");
        pr.println("");
        pr.println("VPN_USER=\"" + username + "\"");
        pr.println("VPN_HOST=" + userVpn.getHost());
        pr.println("VPN_PORT=" + userVpn.getPort());
        pr.println("VPN_NAME=\"" + vpnName + "\"");
        pr.println("CERTS_DIR=$HOME/.cert");
        pr.println("VPN_CA=$CERTS_DIR/${VPN_HOST}-ca.pem");
        pr.println("VPN_USER_CERT=$CERTS_DIR/${VPN_USER}.crt");
        pr.println("VPN_USER_KEY=$CERTS_DIR/${VPN_USER}.key");
        pr.println("RESTORECON=\"$( which restorecon 2>/dev/null )\"");
        pr.println("");
        pr.println("mkdir -p $CERTS_DIR");
        pr.println("cat <<EOF > $VPN_CA");
        pki.writeCaCert(pr);
        pr.println("EOF");
        if (writeUserCert) {
            Pki.KeyAndCert kac = pki.getUserKeyAndCert(username);

            pr.println("cat <<EOF > $VPN_USER_CERT");
            pki.writeCertificate(kac.getCert(), pr);
            pr.println("EOF");

            pr.println("cat <<EOF > $VPN_USER_KEY");
            pki.writePrivateKey(kac.getKey(), pr);
            pr.println("EOF");
            pr.println("chmod 600 $VPN_USER_KEY");
        }
        pr.println("if [ -n \"$RESTORECON\" ]; then restorecon -Rv $CERTS_DIR ; fi");
        pr.println("if nmcli con show | grep -q \"$VPN_NAME\" ; then");
        pr.println("nmcli connection modify \"$VPN_NAME\" vpn.service-type org.freedesktop.NetworkManager.openvpn");
        pr.println("nmcli connection modify \"$VPN_NAME\" vpn.data \"" + vpnOpts.toString() + "\"");
        pr.println("nmcli connection modify \"$VPN_NAME\" ipv4.never-default yes");
        pr.println("nmcli connection modify \"$VPN_NAME\" connection.permissions user:$USER");
        pr.println("nmcli connection modify \"$VPN_NAME\" connection.autoconnect no");
        pr.println("else");
        pr.println("cat <<EOF | nmcli -a con edit");
        pr.println("vpn");
        pr.println("set vpn.service-type org.freedesktop.NetworkManager.openvpn");
        pr.println("set vpn.data " + vpnOpts.toString());
        pr.println("set ipv4.never-default yes");
        pr.println("set connection.id $VPN_NAME");
        pr.println("set connection.permissions user:$USER");
        pr.println("set connection.autoconnect no");
        pr.println("save");
        pr.println("quit");
        pr.println("EOF");
        pr.println("fi");
    }

    public StreamedContent getDownloadNetworkManagerConfig(String username)
        throws AbstractMethodError, ClassNotFoundException, GeneralSecurityException,
            IOException, OperatorCreationException, SQLException
    {
        InputStream in;

        StringWriter writer = new StringWriter();
        writeUserVpnNetworkManagerConfig(writer, username);

        in = new ByteArrayInputStream(writer.toString().getBytes());

        StreamedContent sc = DefaultStreamedContent.builder()
                .name("client-config.sh")
                .contentType("text/plain")
                .stream(() -> in)
                .build();

        return sc;
    }

    public StreamedContent getDownloadOpenVpnConfig(String username)
        throws IOException, CertificateEncodingException, OperatorCreationException,
            AbstractMethodError

    {
        InputStream in;

        StringWriter writer = new StringWriter();
        writeUserVpnClientConfig(writer, username);

        in = new ByteArrayInputStream(writer.toString().getBytes());

        StreamedContent sc = DefaultStreamedContent.builder()
                .name("client-config.ovpn")
                .contentType("text/x-shellscript")
                .stream(() -> in)
                .build();

        return sc;
    }
}
