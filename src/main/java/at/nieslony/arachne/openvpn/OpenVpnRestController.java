/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.firewall.FirewallBasicsSettings;
import static at.nieslony.arachne.openvpn.OpenVpnUserSettings.PasswordVerificationType.HTTP_URL;
import static at.nieslony.arachne.openvpn.OpenVpnUserSettings.PasswordVerificationType.PAM;
import at.nieslony.arachne.openvpnmanagement.OpenVpnManagement;
import at.nieslony.arachne.openvpnmanagement.OpenVpnManagementException;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.pki.PkiNotInitializedException;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.utils.NetUtils;
import at.nieslony.arachne.utils.TransportProtocol;
import jakarta.annotation.security.RolesAllowed;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author claas
 */
@RestController
@RequestMapping("/api/openvpn")
@PropertySource("classpath:arachne.properties")
public class OpenVpnRestController {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnRestController.class);

    @Autowired
    Settings settings;

    @Autowired
    Pki pki;

    @Autowired
    OpenVpnManagement openVpnManagement;

    @Autowired
    private FolderFactory folderFactory;

    @Value("${plugin_path}")
    String pluginPath;

    @GetMapping("/user_settings")
    @RolesAllowed(value = {"ADMIN"})
    public OpenVpnUserSettings get_user_settings() {
        return new OpenVpnUserSettings(settings);
    }

    static final String FN_OPENVPN_SERVER_CONF = "openvpn-user-server.conf";
    static final String FN_OPENVPN_PLUGIN_CONF = "openvpn-plugin-arachne.conf";

    @PostMapping("/user_settings")
    @RolesAllowed(value = {"ADMIN"})
    public OpenVpnUserSettings post_user_settings(
            @RequestBody OpenVpnUserSettings vpnSettings
    ) {
        logger.info("Set new openVPN user server config: " + settings.toString());
        vpnSettings.save(settings);
        writeOpenVpnUserServerConfig(vpnSettings);
        return vpnSettings;
    }

    @GetMapping("/user_config/{username}")
    @RolesAllowed(value = {"ADMIN"})
    public String userVpnConfig(
            @PathVariable String username,
            @RequestParam(required = false, name = "format") String format
    ) {
        try {
            if (format == null) {
                return openVpnUserConfig(username);
            }
            logger.info("Return format: " + format);
            return switch (format) {
                case "json" ->
                    openVpnUserConfigJson(username);
                case "shell" ->
                    openVpnUserConfigShell(username);
                default ->
                    throw new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Cannot get user config");
            };
        } catch (PkiNotInitializedException ex) {
            logger.error("Cannot create user config: " + ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot get user config");
        }
    }

    @GetMapping("/user_config")
    @RolesAllowed(value = {"USER"})
    public String userVpnConfig(
            @RequestParam(required = false, name = "format") String format
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userVpnConfig(username, format);
    }

    public void writeOpenVpnPluginConfig(
            OpenVpnUserSettings openVpnSettings,
            FirewallBasicsSettings firewallBasicsSettings
    ) {
        String fileName = folderFactory.getVpnConfigDir(FN_OPENVPN_PLUGIN_CONF);
        logger.info("Writing openvpn-plugin-arache config to " + fileName);

        try (FileWriter fw = new FileWriter(fileName)) {
            PrintWriter writer = new PrintWriter(fw);
            writer.println("# Generated by Arache, do not modify");
            writer.println("auth-url = %s/api/auth".formatted(openVpnSettings.getAuthHttpUrl()));
            writer.println("enable-firewall = " + firewallBasicsSettings.isEnableFirewall());
            if (firewallBasicsSettings.isEnableFirewall()) {
                writer.println("firewall-zone = " + firewallBasicsSettings.getFirewallZone());
                writer.println(
                        "firewall-url = %s/api/firewall/rules"
                                .formatted(openVpnSettings.getAuthHttpUrl())
                );
            }
            writer.println("enable-routing = "
                    + firewallBasicsSettings.getEnableRoutreMode().name()
            );
        } catch (IOException ex) {
            logger.error(
                    "Cannot write to %s: %s"
                            .formatted(fileName, ex.getMessage())
            );
        }
    }

    public void writeOpenVpnUserServerConfig(OpenVpnUserSettings settings) {
        String fileName = folderFactory.getVpnConfigDir(FN_OPENVPN_SERVER_CONF);
        logger.info("Writing openvpn user server config to " + fileName);

        try (FileWriter fw = new FileWriter(fileName)) {
            PrintWriter writer = new PrintWriter(fw);
            writer.println("# Generated by Arachne on %s".formatted(new Date().toString()));
            writer.println("server %s %s"
                    .formatted(
                            settings.getClientNetwork(),
                            NetUtils.maskLen2Mask(settings.getClientMask())
                    )
            );
            writer.println("local %s".formatted(settings.getListenIp()));
            writer.println("proto %s".formatted(
                    settings.getListenProtocol().name().toLowerCase())
            );
            writer.println("port %d".formatted(settings.getListenPort()));
            writer.println("dev-type %s".formatted(settings.getDeviceType()));
            writer.println("dev %s".formatted(settings.getDeviceName()));
            writer.println("keepalive %d %d"
                    .formatted(
                            settings.getKeepaliveInterval(),
                            settings.getKeepaliveTimeout()));
            writer.println("topology subnet");
            writer.println(openVpnManagement.getVpnConfigSetting());
            for (String dnsServer : settings.getPushDnsServers()) {
                writer.println("push \"dhcp-option DNS " + dnsServer + "\"");
            }
            for (String route : settings.getPushRoutes()) {
                String[] components = route.split("/");
                if (components.length == 2) {
                    components[1] = NetUtils.maskLen2Mask(Integer.parseInt(components[1]));
                    writer.println(
                            "push \"route %s %s\""
                                    .formatted(components[0], components[1])
                    );
                } else {
                    logger.warn("Invalid route: " + route);
                }
            }
            if (settings.getAuthType() == OpenVpnUserSettings.AuthType.USERNAME_PASSWORD) {
                writer.println("client-cert-not-required");
            }
            if (settings.getAuthType() != OpenVpnUserSettings.AuthType.CERTIFICATE) {
                switch (settings.getPasswordVerificationType()) {
                    case HTTP_URL -> {
                        writer.println(
                                "plugin %s %s"
                                        .formatted(
                                                findPlugin("arachne.so"),
                                                folderFactory.getVpnConfigDir(FN_OPENVPN_PLUGIN_CONF)
                                        )
                        );
                    }
                    case PAM -> {
                        writer.println(
                                "plugin %s %s".formatted(
                                        findPlugin("openvpn-plugin-auth-pam.so"),
                                        settings.getAuthPamService()
                                )
                        );
                    }
                }
            }

            writer.println("<ca>\n%s</ca>".formatted(pki.getRootCertAsBase64()));
            writer.println("<cert>\n%s</cert>".formatted(pki.getServerCertAsBase64()));
            writer.println("<key>\n%s</key>".formatted(pki.getServerKeyAsBase64()));
            writer.println("<dh>\n%s</dh>".formatted(pki.getDhParams()));
            openVpnManagement.restartServer();
        } catch (PkiNotInitializedException ex) {
            logger.error("# pki not yet initialized");
        } catch (IOException ex) {
            logger.error(
                    "Cannot write to %s: %s"
                            .formatted(fileName, ex.getMessage())
            );
        } catch (OpenVpnManagementException ex) {
            logger.error("Cannot restart openVPN: " + ex.getMessage());
        }
    }

    public String openVpnUserConfig(String username) throws PkiNotInitializedException {
        OpenVpnUserSettings vpnSettings = new OpenVpnUserSettings(settings);

        String userCert = pki.getUserCertAsBase64(username);
        String privateKey = pki.getUserKeyAsBase64(username);
        String caCert = pki.getRootCertAsBase64();

        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        writer.println("# Generated by Arachne on %s\n".formatted(new Date().toString()));
        writer.println("client");
        writer.println("dev-type %s".formatted(vpnSettings.getDeviceType()));
        writer.println("dev %s".formatted(vpnSettings.getDeviceName()));
        writer.println("proto %s".formatted(
                vpnSettings.getListenProtocol().name().toLowerCase())
        );
        writer.println("remote %s %d".formatted(vpnSettings.getRemote(), vpnSettings.getListenPort()));
        if (vpnSettings.getAuthType() != OpenVpnUserSettings.AuthType.CERTIFICATE) {
            writer.println("auth-user-pass");
        }
        writer.println();
        writer.println("<ca>\n%s\n</ca>".formatted(caCert));
        writer.println("<cert>\n%s\n</cert>".formatted(userCert));
        writer.println("<key>\n%s</key>".formatted(privateKey));

        return sw.toString();
    }

    public String openVpnUserConfigShell(String username) throws PkiNotInitializedException {
        OpenVpnUserSettings vpnSettings = new OpenVpnUserSettings(settings);
        String userCert = pki.getUserCertAsBase64(username);
        String privateKey = pki.getUserKeyAsBase64(username);
        String caCert = pki.getRootCertAsBase64();
        String userCertFn = "~/.certs/arachne-%s.crt".formatted(username);
        String caCertFn = "~/.certs/arachne-%s.crt".formatted(vpnSettings.getRemote());
        String privateKeyFn = "~/.certs/arachne-%s.key".formatted(username);
        int port = vpnSettings.getListenPort();

        StringWriter configWriter = new StringWriter();
        configWriter.append("mkdir -v ~/.certs\n");
        configWriter.append("""
                            cat <<EOF > %s
                            %s
                            EOF
                            """.formatted(caCertFn, caCert));
        configWriter.append("""
                            cat -v <<EOF > %s
                            %s
                            EOF
                            """.formatted(userCertFn, userCert));
        configWriter.append("""
                            cat -v <<EOF > %s
                            %s
                            EOF
                            chmod -v 600 %s
                               """.formatted(privateKeyFn, privateKey, privateKeyFn));
        String conName = vpnSettings.getFormattedClientConfigName(username);
        /*  ca = /home/claas/.certs/arachne-ca.crt,
        cert = /home/claas/.certs/arachne-cert.crt,
        cert-pass-flags = 4, connection-type = password-tls,
        key = /home/claas/.certs/arachne-cert.key,
        password-flags = 2, port = , remote = odysseus.nieslony.lan,
        username = claas@NIESLONY.LAN
         */
        configWriter.append(
                """
                vpn_data="
                    ca = %s,
                    cert = %s,
                    cert-pass-flags = 4,
                    connection-type = password-tls,
                    key = %s,
                    password-flags = 2,
                    port = %s,
                    remote = %s,
                    username = %s
                "
                nmcli connection add type vpn vpn-type openvpn con-name "%s" vpn.data "$vpn_data"
                """
                        .formatted(
                                caCertFn,
                                userCertFn,
                                privateKeyFn,
                                port,
                                vpnSettings.getRemote(),
                                username,
                                conName
                        )
        );
        return configWriter.toString();
    }

    String openVpnUserConfigJson(String username) throws PkiNotInitializedException {
        OpenVpnUserSettings vpnSettings = new OpenVpnUserSettings(settings);

        String userCert = pki.getUserCertAsBase64(username);
        String privateKey = pki.getUserKeyAsBase64(username);
        String caCert = pki.getRootCertAsBase64();

        JSONObject certs = new JSONObject();
        certs.put("userCert", userCert);
        certs.put("privateKey", privateKey);
        certs.put("caCert", caCert);

        JSONObject connection = new JSONObject();
        connection.put("remote", vpnSettings.getRemote());
        connection.put("username", username);
        connection.put("cert-pass-flags", "4");
        connection.put("connection-type", "password-tls");
        connection.put("password-flags", "2");
        connection.put("port", vpnSettings.getListenPort());
        connection.put("dev-type", vpnSettings.getDeviceType());
        if (vpnSettings.getListenProtocol() == TransportProtocol.TCP) {
            connection.put("proto-tcp", "yes");
        }

        JSONObject json = new JSONObject();
        String conName = vpnSettings.getVpnName()
                .replaceAll("%h", vpnSettings.getRemote())
                .replaceAll("%u", username);
        json.put("name", conName);
        json.put("certificates", certs);
        json.put("connection", connection);

        return json.toString(2) + "\n";
    }

    private String findPlugin(String pluginName) {
        for (String dir : pluginPath.split(":")) {
            Path fn = Path.of(dir, pluginName);
            if (Files.exists(fn)) {
                return fn.toString();
            }
        }

        logger.error(
                "Plugin %s not found in search path %s"
                        .formatted(pluginName, pluginPath)
        );
        return "";
    }
}
