/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.CurrentUser;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import org.bouncycastle.operator.OperatorCreationException;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author claas
 */
@ManagedBean
@SessionScoped
public class ConfigBuilder implements Serializable {
    private static final transient Logger logger= Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{pki}")
    private Pki pki;

    @ManagedProperty(value = "#{currentUser}")
    CurrentUser currentUser;

    @ManagedProperty(value = "#{folderFactory}")
    FolderFactory folderFactory;

    @ManagedProperty(value = "#{userVpn}")
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

    public void setCurrentUser(CurrentUser cub) {
        currentUser = cub;
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

    public void writeUserVpnServerConfig(Writer wr)
            throws CertificateEncodingException, IOException
    {
        int scriptSecurity = 1;
        String authScript = folderFactory.getBinDir() + "/auth.sh";

        Path path = Paths.get(authScript);
        try {
            logger.info(String.format("Setting write permissions for %s", authScript));
            PosixFileAttributes attrs = Files.readAttributes(path, PosixFileAttributes.class);
            Set<PosixFilePermission> attrSet = attrs.permissions();
            attrSet.add(PosixFilePermission.OTHERS_EXECUTE);
            attrSet.add(PosixFilePermission.GROUP_EXECUTE);
            attrSet.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(path, attrSet);
        }
        catch (IOException ex) {
            logger.severe(String.format("Error setting write permissions for %s: %s",
                authScript, ex.getMessage()));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\"")
                .append(authScript)
                .append(" ")
                .append(userVpn.getAuthScriptUrl())
                .append("/AuthOpenVPN.xhtml")
                .append("\"");
        String authCmd = sb.toString();
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
                if (scriptSecurity < 2)
                    scriptSecurity = 2;
                pr.println("auth-user-pass-verify " + authCmd + " via-file");
                break;
            case USERPWD:
                if (scriptSecurity < 2)
                    scriptSecurity = 2;
                pr.println("auth-user-pass-verify " + authCmd + " via-file");
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

        if (scriptSecurity != 1) {
            pr.println("script-security " + String.valueOf(scriptSecurity));
        }
    }

    public void writeUserVpnNetworkManagerConfig(Writer wr, String username)
        throws ClassNotFoundException, GeneralSecurityException, IOException,
            OperatorCreationException, AbstractMethodError, SQLException
    {
        boolean writeUserCert = userVpn.getAuthType() != UserVpnBase.VpnAuthType.USERPWD.USERPWD;

        PrintWriter pr = new PrintWriter(wr);

        String vpnName = userVpn.getNmConnectionTemplate();
        vpnName = vpnName
                .replaceAll("%u", username)
                .replaceAll("%h", userVpn.getHost())
                .replaceAll("%n", userVpn.getConnectionName());

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
            writeUserVpnClientConfig(wr, username);
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
            writeUserVpnNetworkManagerConfig(wr, username);
        }
        catch (ClassNotFoundException | GeneralSecurityException | IOException | SQLException ex) {
            logger.warning(String.format("Error getting network manager configuration for user %s: %s",
                    username, ex.getMessage()));

            PrintWriter pw = new PrintWriter(wr);
            pw.println("# Error getting configuration.");
        }
    }

    public StreamedContent getDownloadNetworkManagerConfig(String username)
        throws AbstractMethodError, ClassNotFoundException, GeneralSecurityException,
            IOException, OperatorCreationException, SQLException
    {
        InputStream in;

        StringWriter writer = new StringWriter();
        writeUserVpnNetworkManagerConfig(writer, username);

        in = new ByteArrayInputStream(writer.toString().getBytes());

        StreamedContent sc = new DefaultStreamedContent(in,
                "text/plain", "client-config.sh");

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

        StreamedContent sc = new DefaultStreamedContent(in,
                "text/plain", "openvpn.ovpn");

        return sc;
    }
}
