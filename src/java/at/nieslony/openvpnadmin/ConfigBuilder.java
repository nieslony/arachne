/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.CurrentUser;
import at.nieslony.openvpnadmin.beans.FolderFactory;
import at.nieslony.openvpnadmin.beans.LdapSettings;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.UserVPNBean;
import static at.nieslony.openvpnadmin.beans.UserVPNBean.PROP_AUTH_TYPE;
import at.nieslony.utils.NetUtils;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
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

    @ManagedProperty(value = "#{ldapSettings}")
    private LdapSettings ldapSettings;

    @ManagedProperty(value = "#{folderFactory}")
    FolderFactory folderFactory;

    public void setFolderFactory(FolderFactory ff) {
        this.folderFactory = ff;
    }

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    public void setCurrentUser(CurrentUser cub) {
        currentUser = cub;
    }

    public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    public ConfigBuilder()
    {
    }

    public static void writeUserVpnClientConfig(Properties props,
            Pki pki,
            Writer wr,
            String username)
    throws IOException, CertificateEncodingException {
        PrintWriter pr = new PrintWriter(wr);
        UserVPNBean.VpnAuthType authType = UserVPNBean.VpnAuthType.valueOf(props.getProperty(PROP_AUTH_TYPE));

        pr.println("# openVPN config for user " + username);
        pr.println("client");
        pr.println("remote " + props.getProperty(UserVPNBean.PROP_HOST));
        pr.println("port " + props.getProperty(UserVPNBean.PROP_PORT));
        pr.println("dev " + props.getProperty(UserVPNBean.PROP_DEVICE_TYPE).toLowerCase());
        pr.println("proto " + props.getProperty(UserVPNBean.PROP_PROTOCOL).toLowerCase());

        if (authType != UserVPNBean.VpnAuthType.AUTH_CLIENT_CERT) {
            pr.println("auth-user-pass");
        }

        pr.println();
        pr.println("<ca>");
        pki.writeCaCert(pr);
        pr.println("</ca>");

        if (authType != UserVPNBean.VpnAuthType.AUTH_USERNAME_PASSWORD) {
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
                pki.writeCertificate(kac.getCert(), pr);
                pr.println("</key>");
            }
        }
    }

    public void writeUserVpnServerConfig(Properties props, Pki pki, Writer wr)
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
                .append(props.getProperty(UserVPNBean.PROP_AUTH_SCRIPT_URL))
                .append("/AuthOpenVPN.xhtml")
                .append("\"");
        String authCmd = sb.toString();
        PrintWriter pr = new PrintWriter(wr);

        pr.println("port " + props.getProperty(UserVPNBean.PROP_PORT));
        pr.println("proto " + props.getProperty(UserVPNBean.PROP_PROTOCOL).toLowerCase());
        pr.println("dev " + props.getProperty(UserVPNBean.PROP_DEVICE_TYPE).toLowerCase());
        pr.println("server " + props.getProperty(UserVPNBean.PROP_CLIENT_NETWORK) +
                " " + NetUtils.maskLen2Mask(
                        Integer.parseInt(props.getProperty(UserVPNBean.PROP_CLIENT_NET_MASK))
                        )
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
        pr.println("keepalive " + props.getProperty(UserVPNBean.PROP_PING) +
                " " + props.getProperty(UserVPNBean.PROP_PING_RESTART));
        pr.println("management 127.0.0.1 9544");
        switch (UserVPNBean.VpnAuthType.valueOf(props.getProperty(UserVPNBean.PROP_AUTH_TYPE))) {
            case AUTH_BOTH:
                if (scriptSecurity < 2)
                    scriptSecurity = 2;
                pr.println("auth-user-pass-verify " + authCmd + " via-file");
                break;
            case AUTH_USERNAME_PASSWORD:
                if (scriptSecurity < 2)
                    scriptSecurity = 2;
                pr.println("auth-user-pass-verify " + authCmd + " via-file");
                pr.println("client-cert-not-required");
                pr.println("username-as-common-name");
        }
        String dnsServers = props.getProperty(UserVPNBean.PROP_DNS_SERVERS);
        if (dnsServers != null && !dnsServers.isEmpty()) {
            for (String dns : dnsServers.split(",")) {
                if (!dns.isEmpty())
                    pr.println("push \"dhcp-option DNS " + dns + "\"");
            }

        }
        String routes = props.getProperty(UserVPNBean.PROP_PUSH_ROUTES);
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

    public static void writeUserVpnNetworkManagerConfig(Properties props, Pki pki, Writer wr, String username)
        throws IOException, CertificateEncodingException, ClassNotFoundException, GeneralSecurityException, SQLException
    {
        UserVPNBean.VpnAuthType authType = UserVPNBean.VpnAuthType.valueOf(props.getProperty(PROP_AUTH_TYPE));
        boolean writeUserCert = authType != UserVPNBean.VpnAuthType.AUTH_USERNAME_PASSWORD;

        PrintWriter pr = new PrintWriter(wr);
        StringWriter vpnName = new StringWriter();
        vpnName
                .append(props.getProperty(UserVPNBean.PROP_NAME))
                .append(" - ")
                .append(username)
                .append("@")
                .append(props.getProperty(UserVPNBean.PROP_HOST));
        StringBuilder vpnOpts = new StringBuilder();
        vpnOpts.append("remote = $VPN_HOST");
        vpnOpts.append(", port = $VPN_PORT");
        vpnOpts.append(", ca = $VPN_CA");
        vpnOpts.append(", password-flags = 1");

        switch (authType) {
            case AUTH_BOTH:
                vpnOpts.append(", connection-type = password-tls")
                        .append(", cert-pass-flags = 4");
                break;
            case AUTH_CLIENT_CERT:
                vpnOpts.append(", connection-type = tls");
                break;
            case AUTH_USERNAME_PASSWORD:
                vpnOpts.append(", connection-type = password");
                break;
        }

        if (authType != UserVPNBean.VpnAuthType.AUTH_USERNAME_PASSWORD) {
            vpnOpts.append(", cert = $VPN_USER_CERT");
            vpnOpts.append(", key = $VPN_USER_KEY");
        }

        vpnOpts.append(", username = $VPN_USER");
        if (props.getProperty(UserVPNBean.PROP_PROTOCOL).equalsIgnoreCase("TCP"))
            vpnOpts.append(", proto-tcp = yes");

        pr.println("#!/bin/bash");
        pr.println("");
        pr.println("VPN_USER=\"" + username + "\"");
        pr.println("VPN_HOST=" + props.getProperty(UserVPNBean.PROP_HOST));
        pr.println("VPN_PORT=" + props.getProperty(UserVPNBean.PROP_PORT));
        pr.println("VPN_NAME=\"" + vpnName.toString() + "\"");
        pr.println("CERTS_DIR=$HOME/.cert");
        pr.println("VPN_CA=$CERTS_DIR/${VPN_HOST}-ca.pem");
        pr.println("VPN_USER_CERT=$CERTS_DIR/${VPN_USER}.crt");
        pr.println("VPN_USER_KEY=$CERTS_DIR/${VPN_USER}.key");
        pr.println("");
        pr.println("mkdir -p $CERTS_DIR");
        pr.println("restorecon -Rv $CERTS_DIR");
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
        pr.println("restorecon -R $CERTS_DIR");

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

    public void networkManagerConfig(ComponentSystemEvent event) {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        Map<String, String> params = ec.getRequestParameterMap();

        try {
            String fn = folderFactory.getUserVpnPath("0");
            FileReader in = new FileReader(fn);
            Properties props = new Properties();
            props.load(in);

            Writer wr = ec.getResponseOutputWriter();

            ec.setResponseContentType("text/plain");
            ec.setResponseCharacterEncoding("UTF-8");
            writeUserVpnNetworkManagerConfig(props, pki, wr, currentUser.getUsername());
        }
        catch (ClassNotFoundException | GeneralSecurityException | IOException | SQLException ex) {
            logger.severe(ex.getMessage());
        }

        fc.responseComplete(); // Important! Prevents JSF from proceeding to render HTML.
    }

    public void ovpnConfig(ComponentSystemEvent event) {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        Map<String, String> params = ec.getRequestParameterMap();

        try {
            Writer wr = ec.getResponseOutputWriter();
            ec.setResponseContentType("text/plain");
            ec.setResponseCharacterEncoding("UTF-8");

            String fn = folderFactory.getUserVpnPath("0");
            FileReader in;
            Properties props = null;
            try {
                in = new FileReader(fn);
                props = new Properties();
                props.load(in);
            }
            catch (FileNotFoundException ex) {
                logger.warning("Writing empty config");
                wr.write("# No client VPN configured yet.\n");
            }
            if (props != null && !props.isEmpty())
                writeUserVpnClientConfig(props, pki, wr, currentUser.getUsername());
        }
        catch (CertificateEncodingException | IOException ex) {
            logger.severe(ex.getMessage());
        }

        fc.responseComplete(); // Important! Prevents JSF from proceeding to render HTML.
    }

    public StreamedContent getDownloadNetworkManagerConfig(String username)
            throws ClassNotFoundException, GeneralSecurityException, IOException, SQLException
    {
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(folderFactory.getUserVpnPath("0"));
        props.load(fis);

        InputStream in;

        StringWriter writer = new StringWriter();
        ConfigBuilder.writeUserVpnNetworkManagerConfig(props, pki, writer, username);

        in = new ByteArrayInputStream(writer.toString().getBytes());

        StreamedContent sc = new DefaultStreamedContent(in,
                "text/plain", "client-config.ovpn");

        return sc;
    }

    public StreamedContent getDownloadOpenVpnConfig(String username)
            throws IOException, CertificateEncodingException {
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(folderFactory.getUserVpnPath("0"));
        props.load(fis);

        InputStream in;

        StringWriter writer = new StringWriter();
        ConfigBuilder.writeUserVpnClientConfig(props, pki, writer, username);

        in = new ByteArrayInputStream(writer.toString().getBytes());

        StreamedContent sc = new DefaultStreamedContent(in,
                "text/plain", "openvpn.ovpn");

        return sc;
    }
}
