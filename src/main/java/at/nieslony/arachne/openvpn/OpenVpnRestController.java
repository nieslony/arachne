/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.firewall.FirewallBasicsSettings;
import static at.nieslony.arachne.openvpn.OpenVpnUserSettings.PasswordVerificationType.HTTP_URL;
import static at.nieslony.arachne.openvpn.OpenVpnUserSettings.PasswordVerificationType.PAM;
import at.nieslony.arachne.pki.CertificateRepository;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509CRL;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.json.JSONException;
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
    private Settings settings;

    @Autowired
    private Pki pki;

    @Autowired
    private FolderFactory folderFactory;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private VpnSiteController vpnSiteController;

    @Autowired
    private VpnSiteRepository vpnSiteRepository;

    @Value("${plugin_path}")
    String pluginPath;

    @GetMapping("/user_settings")
    @RolesAllowed(value = {"ADMIN"})
    public OpenVpnUserSettings getUserSettings() {
        return settings.getSettings(OpenVpnUserSettings.class);
    }

    static final String FN_OPENVPN_USER_SERVER_CONF = "openvpn-user-server.conf";
    static final String FN_OPENVPN_SITE_SERVER_CONF = "openvpn-site-server.conf";
    static final String FN_OPENVPN_PLUGIN_USER_CONF = "openvpn-plugin-arachne-user.conf";
    static final String FN_OPENVPN_PLUGIN_SITE_CONF = "openvpn-plugin-arachne-site.conf";
    static final String FN_OPENVPN_CRL = "crl.pem";
    static final String FN_OPENVPN_CLIENT_CONF_DIR = "site-client-conf.d";

    @PostConstruct
    public void init() {
        writeCrl();
    }

    public void writeCrl() {
        if (certificateRepository.count() > 0) {
            X509CRL crl = pki.getCrl(() -> {
                return certificateRepository.findByRevocationDateIsNotNullOrderByValidToDesc();
            });

            String crlString = Pki.asBase64(crl);
            String fn = folderFactory.getVpnConfigDir("crl.pem");
            try (FileWriter fw = new FileWriter(fn)) {
                fw.write(crlString);
            } catch (IOException ex) {
                logger.error("Cannot write %s: %s".formatted(fn, ex.getMessage()));
            }
        }
    }

    @PostMapping("/user_settings")
    @RolesAllowed(value = {"ADMIN"})
    public OpenVpnUserSettings postUserSettings(
            @RequestBody OpenVpnUserSettings vpnSettings
    ) throws SettingsException {
        logger.info("Set new openVPN user server config: " + settings.toString());
        vpnSettings.save(settings);
        writeOpenVpnUserServerConfig(vpnSettings);
        return vpnSettings;
    }

    @GetMapping("/user_config/{username}")
    @RolesAllowed(value = {"ADMIN"})
    public String getUserVpnConfig(
            @PathVariable String username,
            @RequestParam(required = false, name = "format") String format
    ) throws SettingsException {
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
        } catch (PkiException | JSONException ex) {
            logger.error("Cannot create user config: " + ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot get user config");
        }
    }

    @GetMapping("/user_config")
    @RolesAllowed(value = {"USER"})
    public String getUserVpnConfig(
            @RequestParam(required = false, name = "format") String format
    ) throws SettingsException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return getUserVpnConfig(username, format);
    }

    @GetMapping("/site")
    @RolesAllowed(value = {"ADMIN"})
    public List<VpnSite> getSiteVpnSite() {
        return vpnSiteRepository.findAll();
    }

    @GetMapping("/site/{id}")
    @RolesAllowed(value = {"ADMIN"})
    public VpnSite getSiteVpnSite(@PathVariable Long id) {
        return vpnSiteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "VPN Site %d not found".formatted(id)));
    }

    private void writeConfigHeader(PrintWriter pw) {
        Date now = new Date();
        pw.println("# Generated by Arachne on %s, do not modify".formatted(now.toString()));
        pw.println("#");
    }

    private String getSitePluginConfTemplate() {
        return folderFactory.getVpnConfigDir(FN_OPENVPN_CLIENT_CONF_DIR + "/plugin_%cn.conf");
    }

    private String getSitePluginConf(String hostname) {
        return getSitePluginConfTemplate().replaceFirst("%cn", hostname);
    }

    public void writeOpenVpnPluginSiteConfig() {
        OpenVpnSiteSettings siteSettings = settings.getSettings(OpenVpnSiteSettings.class);
        String fileName = folderFactory.getVpnConfigDir(FN_OPENVPN_PLUGIN_SITE_CONF);
        FirewallBasicsSettings firewallBasicSettings = settings.getSettings(FirewallBasicsSettings.class);
        logger.info("Writing openvpn-plugin-arache config to " + fileName);
        try (FileWriter fw = new FileWriter(fileName)) {
            PrintWriter writer = new PrintWriter(fw);
            writeConfigHeader(writer);
            writer.println("enable-routing = " + firewallBasicSettings.getEnableRoutingMode().name());
            writer.println("client-config = " + getSitePluginConfTemplate());
        } catch (IOException ex) {
            logger.error(
                    "Cannot write to %s: %s"
                            .formatted(fileName, ex.getMessage())
            );
        }
    }

    public void writeOpenVpnPluginUserConfig(
            OpenVpnUserSettings openVpnSettings,
            FirewallBasicsSettings firewallBasicsSettings
    ) {
        String fileName = folderFactory.getVpnConfigDir(FN_OPENVPN_PLUGIN_USER_CONF);
        logger.info("Writing openvpn-plugin-arache config to " + fileName);

        try (FileWriter fw = new FileWriter(fileName)) {
            PrintWriter writer = new PrintWriter(fw);
            writeConfigHeader(writer);
            writer.println("auth-url = %s/api/auth".formatted(openVpnSettings.getAuthHttpUrl()));
            writer.println("enable-firewall = " + firewallBasicsSettings.isEnableFirewall());
            if (firewallBasicsSettings.isEnableFirewall()) {
                writer.println("firewall-zone = " + firewallBasicsSettings.getFirewallZone());
                writer.println(
                        "firewall-url = %s/api/firewall"
                                .formatted(openVpnSettings.getAuthHttpUrl())
                );
            }
            writer.println("enable-routing = "
                    + firewallBasicsSettings.getEnableRoutingMode().name()
            );
        } catch (IOException ex) {
            logger.error(
                    "Cannot write to %s: %s"
                            .formatted(fileName, ex.getMessage())
            );
        }
    }

    public void writeOpenVpnUserServerConfig(OpenVpnUserSettings settings) {
        String fileName = folderFactory.getVpnConfigDir(FN_OPENVPN_USER_SERVER_CONF);
        logger.info("Writing openvpn user server config to " + fileName);

        writeCrl();

        try (FileWriter fw = new FileWriter(fileName)) {
            PrintWriter writer = new PrintWriter(fw);
            writeConfigHeader(writer);
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
            if (settings.getListenProtocol() == TransportProtocol.UDP && settings.getMtuTest()) {
                writer.println("mtu-test");
            }
            writer.println(
                    "status %s %d"
                            .formatted(
                                    folderFactory.getOpenVpnStatusPath("user"),
                                    settings.getStatusUpdateSecs()
                            )
            );
            writer.println("status-version 2");
            writer.println("writepid " + folderFactory.getOpenVpnPidPath("user"));
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
                        writer.println("plugin %s %s"
                                .formatted(findPlugin("arachne.so"),
                                        folderFactory.getVpnConfigDir(FN_OPENVPN_PLUGIN_USER_CONF)
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

            writer.println("crl-verify " + folderFactory.getVpnConfigDir(FN_OPENVPN_CRL));
            writer.println("<ca>\n%s</ca>".formatted(pki.getRootCertAsBase64()));
            writer.println("<cert>\n%s</cert>".formatted(pki.getServerCertAsBase64()));
            writer.println("<key>\n%s</key>".formatted(pki.getServerKeyAsBase64()));
            writer.println("<dh>\n%s</dh>".formatted(pki.getDhParams()));
        } catch (PkiException ex) {
            logger.error("# pki not yet initialized");
        } catch (IOException ex) {
            logger.error(
                    "Cannot write to %s: %s"
                            .formatted(fileName, ex.getMessage())
            );
        } catch (SettingsException ex) {
            logger.error("Settings exception: " + ex.getMessage());
        }
    }

    public String openVpnUserConfig(String username) throws PkiException, SettingsException {
        OpenVpnUserSettings vpnSettings = settings.getSettings(OpenVpnUserSettings.class);

        String userCert = pki.getUserCertAsBase64(username);
        String privateKey = pki.getUserKeyAsBase64(username);
        String caCert = pki.getRootCertAsBase64();

        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        writeConfigHeader(writer);
        writer.println("client");
        writer.println("dev tun");
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

    public String openVpnUserConfigShell(String username) throws PkiException, SettingsException {
        OpenVpnUserSettings vpnSettings = settings.getSettings(OpenVpnUserSettings.class);
        String userCert = pki.getUserCertAsBase64(username);
        String privateKey = pki.getUserKeyAsBase64(username);
        String caCert = pki.getRootCertAsBase64();
        String userCertFn = "$HOME/.cert/arachne-%s.crt".formatted(username);
        String caCertFn = "$HOME/.cert/arachne-ca-%s.crt".formatted(vpnSettings.getRemote());
        String privateKeyFn = "$HOME/.cert/arachne-%s.key".formatted(username);
        int port = vpnSettings.getListenPort();

        StringWriter configWriter = new StringWriter();
        configWriter.append("mkdir -v $HOME/.cert\n");
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
        configWriter.append("vpn_opts=\"\n");
        if (!vpnSettings.getInternetThrouphVpn()) {
            configWriter.append("    ipv4.never-default yes\n");
        }
        configWriter.append(
                "    ipv4.dns-search %s\n"
                        .formatted(String.join(",", vpnSettings.getDnsSearch()))
        );
        configWriter.append(
                "    ipv4.dns %s\n"
                        .formatted(String.join(",", vpnSettings.getPushDnsServers()))
        );
        configWriter.append("\"\n");

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
                nmcli connection add type vpn vpn-type openvpn con-name "%s" $vpn_opts vpn.data "$vpn_data"
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

    String openVpnUserConfigJson(String username) throws JSONException, PkiException, SettingsException {
        OpenVpnUserSettings vpnSettings = settings.getSettings(OpenVpnUserSettings.class);

        String userCert = pki.getUserCertAsBase64(username);
        String privateKey = pki.getUserKeyAsBase64(username);
        String caCert = pki.getRootCertAsBase64();

        JSONObject certs = new JSONObject();
        certs.put("userCert", userCert);
        certs.put("privateKey", privateKey);
        certs.put("caCert", caCert);

        JSONObject data = new JSONObject();
        data.put("remote",
                "%s:%d"
                        .formatted(
                                vpnSettings.getRemote(),
                                vpnSettings.getListenPort()
                        )
        );
        data.put("username", username);
        data.put("cert-pass-flags", "4");
        data.put("connection-type", "password-tls");
        data.put("password-flags", "2");
        data.put("dev-type", vpnSettings.getDeviceType());
        if (vpnSettings.getListenProtocol() == TransportProtocol.TCP) {
            data.put("proto-tcp", "yes");
        }

        JSONObject ipv4 = new JSONObject();
        ipv4.put("never-default", !vpnSettings.getInternetThrouphVpn());
        ipv4.put("dns-search", vpnSettings.getDnsSearch());
        ipv4.put("dns", vpnSettings.getPushDnsServers());

        JSONObject json = new JSONObject();
        String conName = vpnSettings.getVpnName()
                .replaceAll("%h", vpnSettings.getRemote())
                .replaceAll("%u", username);
        json.put("name", conName);
        json.put("certificates", certs);
        json.put("data", data);
        json.put("ipv4", ipv4);

        return json.toString(2) + "\n";
    }

    private String findPlugin(String pluginName) {
        logger.info("Searching for plugin in " + pluginPath);
        for (String dir : pluginPath.split(":")) {
            Path fn = Path.of(dir, pluginName);
            if (Files.exists(fn)) {
                String absFn = fn.toAbsolutePath().toString();
                logger.info("Found plugin: " + absFn);
                return absFn;
            }
        }

        logger.error(
                "Plugin %s not found in search path %s"
                        .formatted(pluginName, pluginPath)
        );
        return "";
    }

    public void prepareSiteClientDir() {
        String clientConfDirName = folderFactory.getVpnConfigDir(FN_OPENVPN_CLIENT_CONF_DIR);
        try {
            Files.createDirectories(Path.of(clientConfDirName));
        } catch (IOException ex) {
            logger.error("Cannot create %s: %s".formatted(clientConfDirName, ex.getMessage()));
            return;
        }
        File clientConfDir = new File(clientConfDirName);
        for (File f : clientConfDir.listFiles()) {
            if (f.isFile()) {
                logger.info("Removing " + f.getPath());
                f.delete();
            }
        }
    }

    public void writeOpenVpnSiteServerSitesPluginConfig() {
        for (VpnSite site : vpnSiteController.getAll()) {
            if (site.isDefaultSite()) {
                continue;
            }
            String fileName = getSitePluginConf(site.getSiteHostname());
            logger.info("Creating plugin site config " + fileName);
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                PrintWriter pw = new PrintWriter(fos);
                writeConfigHeader(pw);
                pw.println("site-verification = " + site.getSiteVerification().name());
                if (site.getSiteVerification() == VpnSite.SiteVerification.WHITELIST) {
                    pw.println("ip-wihtelist = " + String.join(", ", site.getIpWhiteList()));
                }
                pw.close();
                fos.close();
            } catch (IOException ex) {
                logger.error("Cannot write configuration to %s: %s"
                        .formatted(fileName, ex.getMessage())
                );
            }
        }
    }

    public void writeOpenVpnSiteServerSitesConfig() {
        VpnSite defaultSite = vpnSiteController.getDefaultSite();
        String clientConfDirName = folderFactory.getVpnConfigDir(FN_OPENVPN_CLIENT_CONF_DIR);

        for (VpnSite site : vpnSiteController.getNonDefaultSites()) {
            String fileName
                    = "%s/%s".formatted(
                            clientConfDirName,
                            site.getSiteHostname()
                    );
            site.updateInheritedValues(defaultSite);
            logger.info("Creating site configuration " + fileName);
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                PrintWriter pw = new PrintWriter(fos);
                writeConfigHeader(pw);
                for (String dnsServer : site.getPushDnsServers()) {
                    pw.println(
                            "push \"dhcp-option DNS %s\""
                                    .formatted(dnsServer)
                    );
                }
                for (String route : site.getPushRoutes()) {
                    String[] components = route.split("/");
                    if (components.length == 2) {
                        components[1] = NetUtils.maskLen2Mask(Integer.parseInt(components[1]));
                        pw.println(
                                "push \"route %s %s\""
                                        .formatted(components[0], components[1])
                        );
                    } else {
                        logger.warn("Invalid route: " + route);
                    }
                }
                if (site.isRouteInternetThroughVpn()) {
                    pw.println("push \"redirect-gateway\"");
                }
                if (!site.getPushSearchDomains().isEmpty()) {
                    pw.println(
                            "push \"dns search-domains %s\""
                                    .formatted(String.join(" ", site.getPushSearchDomains()))
                    );
                }
                pw.close();
                fos.close();
            } catch (IOException ex) {
                logger.error("Cannot write configuration to %s: %s"
                        .formatted(fileName, ex.getMessage())
                );
            }
        }
    }

    public void writeOpenVpnSiteServerConfig() {
        OpenVpnSiteSettings openVpnSiteSettings = settings.getSettings(OpenVpnSiteSettings.class);
        String fileName = folderFactory.getVpnConfigDir(FN_OPENVPN_SITE_SERVER_CONF);
        String clientConfigDir = folderFactory.getVpnConfigDir(FN_OPENVPN_CLIENT_CONF_DIR);
        logger.info("Writing site VPN configuration to " + fileName);

        try {
            Files.createDirectory(Paths.get(clientConfigDir));
        } catch (FileAlreadyExistsException ex) {
            logger.info("Client config dir %s already exisrs".formatted(clientConfigDir));
        } catch (IOException ex) {
            logger.error(
                    "Cannot create client config dir %s: %s"
                            .formatted(clientConfigDir, ex.getMessage())
            );
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            PrintWriter pw = new PrintWriter(fos);
            writeConfigHeader(pw);
            pw.println(
                    "server %s %s"
                            .formatted(
                                    openVpnSiteSettings.getSiteNetwork(),
                                    NetUtils.maskLen2Mask(openVpnSiteSettings.getSiteNetworkMask())
                            )
            );
            pw.println("local " + openVpnSiteSettings.getListenIp());
            pw.println("proto " + openVpnSiteSettings
                    .getListenProtocol()
                    .toString()
                    .toLowerCase()
            );
            pw.println("port %d".formatted(openVpnSiteSettings.getListenPort()));
            pw.println("dev-type " + openVpnSiteSettings.getDeviceType());
            pw.println("dev " + openVpnSiteSettings.getDeviceName());
            pw.println(
                    "keepalive %d %d"
                            .formatted(
                                    openVpnSiteSettings.getKeepaliveInterval(),
                                    openVpnSiteSettings.getKeepaliveTimeout()
                            )
            );
            pw.println("topology subnet");

            pw.println(
                    "status %s %d"
                            .formatted(
                                    folderFactory.getOpenVpnStatusPath("site"),
                                    60
                            )
            );
            pw.println("status-version 2");
            pw.println("writepid " + folderFactory.getOpenVpnPidPath("site"));

            if (openVpnSiteSettings.getListenProtocol() == TransportProtocol.UDP
                    && openVpnSiteSettings.getMtuTest()) {
                pw.println("mtu-test");
            }
            pw.println("plugin %s %s"
                    .formatted(findPlugin("arachne.so"),
                            folderFactory.getVpnConfigDir(FN_OPENVPN_PLUGIN_SITE_CONF)
                    )
            );

            pw.println("crl-verify " + folderFactory.getVpnConfigDir(FN_OPENVPN_CRL));
            pw.println("client-config-dir " + clientConfigDir);
            pw.println("<ca>\n%s</ca>".formatted(pki.getRootCertAsBase64()));
            pw.println("<cert>\n%s</cert>".formatted(pki.getServerCertAsBase64()));
            pw.println("<key>\n%s</key>".formatted(pki.getServerKeyAsBase64()));
            pw.println("<dh>\n%s</dh>".formatted(pki.getDhParams()));
            pw.close();
        } catch (IOException | PkiException | SettingsException ex) {
            logger.error(
                    "Cannot write configuration to %s: %s"
                            .formatted(fileName, ex.getMessage())
            );
        }
    }

    public void writeOpenVpnSiteRemoteConfig(long siteId, Writer writer) {
        OpenVpnSiteSettings openVpnSiteSettings
                = settings.getSettings(OpenVpnSiteSettings.class);
        Optional<VpnSite> site = vpnSiteController.getById(siteId);
        if (site.isEmpty()) {
            logger.error("Site %i not found".formatted(siteId));
            return;
        }

        try (PrintWriter pw = new PrintWriter(writer)) {
            pw.println(
                    "# Created by Arachne on %s, do not modify"
                            .formatted(new Date().toString())
            );
            pw.println("client");
            pw.println(
                    "remote %s %s %s"
                            .formatted(
                                    openVpnSiteSettings.getConnectToHost(),
                                    openVpnSiteSettings.getListenPort(),
                                    openVpnSiteSettings.getListenProtocol()
                                            .toString()
                                            .toLowerCase()
                            )
            );
            pw.println("dev " + openVpnSiteSettings.getDeviceType());
            pw.println("""
                   <ca>
                   %s
                   </ca>
                   """.formatted(pki.getRootCertAsBase64())
            );
            pw.println("""
                   <cert>
                   %s
                   </cert>
                   """
                    .formatted(pki.getUserCertAsBase64(site.get().getSiteHostname()))
            );
            pw.println("""
                   <key>
                   %s
                   </key>
                   """
                    .formatted(pki.getUserKeyAsBase64(site.get().getSiteHostname()))
            );
        } catch (PkiException | SettingsException ex) {
            logger.error("Cannot write site remote configuration: " + ex.getMessage());
        }
    }

    public String getOpenVpnSiteRemoteConfigFileName(
            OpenVpnSiteSettings siteSettings,
            VpnSite vpnSite
    ) {
        return getOpenVpnSiteRemoteConfigName(siteSettings, vpnSite) + ".conf";
    }

    public String getOpenVpnSiteRemoteConfigName(
            OpenVpnSiteSettings siteSettings,
            VpnSite vpnSite
    ) {
        return "arachne_%s_%s".formatted(siteSettings.getConnectToHost(), vpnSite.getSiteHostname());
    }
}
