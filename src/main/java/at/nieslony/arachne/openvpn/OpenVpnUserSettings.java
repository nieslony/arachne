/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
@Slf4j
public class OpenVpnUserSettings
        extends AbstractSettingsGroup
        implements OpenVpnSettings {

    public enum AuthType {
        CERTIFICATE("Certificate"),
        USERNAME_PASSWORD("Username/Password"),
        USERNAME_PASSWORD_CERTIFICATE("Username/Password + Certificate");

        private AuthType(String authType) {
            this.authType = authType;
        }

        private final String authType;

        @Override
        public String toString() {
            return authType;
        }
    }

    public enum PasswordVerificationType {
        PAM("Pam"),
        HTTP_URL("Http URL");

        private PasswordVerificationType(String pvt) {
            this.pvt = pvt;
        }

        private final String pvt;

        @Override
        public String toString() {
            return pvt;
        }
    }

    public enum NetworkManagerRememberPassword {
        REMEMBER_EVERYBODY(0, "Remember for everybody (unencrypted)"),
        REMEMBER_USER(1, "Remember for user (encrypted)"),
        ALWAYS_ASK(2, "Always ask");

        private NetworkManagerRememberPassword(int cfgValue, String label) {
            this.cfgValue = cfgValue;
            this.label = label;
        }

        private final String label;
        private final int cfgValue;

        @Override
        public String toString() {
            return label;
        }

        public int getCfgValue() {
            return cfgValue;
        }
    }

    public OpenVpnUserSettings() {
    }

    private boolean alreadyConfigured = false;

    private String vpnName = "Arachne OpenVPN - %u@%h";
    private String clientConfigName = "arachne-openVPN-client.conf";
    private String listenIp = "0.0.0.0";
    private int listenPort = 1194;
    private TransportProtocol listenProtocol = TransportProtocol.TCP;
    private Boolean mtuTest = true;
    private String remote = NetUtils.myHostname();
    private String deviceType = "tun";
    private String deviceName = "arachne-user";
    private String clientNetwork = "192.168.131.0";
    private int clientMask = 24;
    private int keepaliveTimeout = 60;
    private int keepaliveInterval = 10;
    private int statusUpdateSecs = 60;
    private String runAsUser;
    private List<String> pushDnsServers = NetUtils.getDnsServers();
    private List<String> dnsSearch = Arrays.asList(NetUtils.myDomain());
    private List<String> pushRoutes = NetUtils.getDefaultPushRoutes();
    private Boolean internetThrouphVpn = false;
    private AuthType authType = AuthType.USERNAME_PASSWORD_CERTIFICATE;
    private PasswordVerificationType passwordVerificationType = PasswordVerificationType.HTTP_URL;
    private String authPamService = "arachne";
    private String authHttpUrl = defaultAuthUrl(
            Settings.getInstance().getServerProperties()
    );
    private NetworkManagerRememberPassword networkManagerRememberPassword = NetworkManagerRememberPassword.ALWAYS_ASK;

    public void setPushDnsServers(List<String> pushDnsServers) {
        log.info(pushDnsServers.toString());
        this.pushDnsServers = new LinkedList<>(pushDnsServers);
        log.info(this.pushDnsServers.toString());
    }

    private String defaultAuthUrl(ServerProperties serverProperties) {
        return "http://%s:%d/arachne".formatted(NetUtils.myHostname(), 8080);
    }

    public String getFormattedClientConfigName(String username) {
        return getVpnName()
                .replaceAll("%h", getRemote())
                .replaceAll("%u", username)
                .replaceAll("%U", username.split("@")[0]);
    }

    @Override
    public void save(Settings settings) throws SettingsException {
        alreadyConfigured = true;
        super.save(settings);
    }
}
