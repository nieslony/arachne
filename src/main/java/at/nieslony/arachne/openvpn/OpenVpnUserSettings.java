/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class OpenVpnUserSettings {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnUserSettings.class);

    private static final String SK_OPENVPN_USER_NAME = "openvpn.user.name";
    private static final String SK_OPENVPN_USER_CLIENT_CFG_NAME = "openvpn.client-config-name";
    private static final String SK_OPENVPN_USER_LISTEN_IP = "openvpn.user.listenIp";
    private static final String SK_OPENVPN_USER_LISTEN_PORT = "openvpn.user.listenPort";
    private static final String SK_OPENVPN_USER_LISTEN_PROTOCOL = "openvpn.user.listenProtocol";
    private static final String SK_OPENVPN_USER_MTU_TEST = "openvpn.user.mtu-test";
    private static final String SK_OPENVPN_USER_REMOTE = "openvpn.user.remote";
    private static final String SK_OPENVPN_USER_DEVICE_TYPE = "openvpn.user.deviceType";
    private static final String SK_OPENVPN_USER_DEVICE_NAME = "openvpn.user.deviceName";
    private static final String SK_OPENVPN_USER_CLIENT_NETWORK = "openvpn.user.clientNetwork";
    private static final String SK_OPENVPN_USER_CLIENT_MASK = "openvpn.user.clientMask";
    private static final String SK_OPENVPN_USER_KEEPALIVE_INTERVAL = "openvpn.user.keepaliveInterval";
    private static final String SK_OPENVPN_USER_KEEPALIVE_TIMEOUT = "openvpn.user.keepaliveTimeout";
    private static final String SK_OPENVPN_USER_PUSH_DNS = "openvpn.user.pushdns";
    private static final String SK_OPENVPN_USER_DNS_SEARCH = "openvpn.user.dns-search";
    private static final String SK_OPENVPN_USER_PUSH_ROUTES = "openvpn.user.pushroutes";
    private static final String SK_OPENVPN_USER_INTERNET_THROUGH_VPN = "openvpn.user.internet-through-vpn";
    private static final String SK_OPENVPN_USER_AUTH_TYPE = "openvpn.user.auth-type";
    private static final String SK_OPENVPN_USER_PWD_VERF_TYPE = "openvpn.user.password-verification-type";
    private static final String SK_OPENVPN_USER_AUTH_PAM_SERVICE = "openvpn.user.auth-pam-service";
    private static final String SK_OPENVPN_USER_AUTH_HTTP_URL = "openvpn.user-auth-http-url";

    public enum AuthType {
        CERTIFICATE("Certificate"),
        USERNAME_PASSWORD("Username/Password"),
        USERNAME_PASSWORD_CERTIFICATE("Username/Password + Certificate");

        private AuthType(String authType) {
            this.authType = authType;
        }

        private String authType;

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

        private String pvt;

        @Override
        public String toString() {
            return pvt;
        }
    }

    public OpenVpnUserSettings() {
    }

    public OpenVpnUserSettings(Settings settings) {
        vpnName = settings.get(SK_OPENVPN_USER_NAME, "Arachne OpenVPN - %u@%h");
        clientConfigName = settings.get(SK_OPENVPN_USER_CLIENT_CFG_NAME, "arachne-openVPN-client.conf");
        listenIp = settings.get(SK_OPENVPN_USER_LISTEN_IP, "0.0.0.0");
        listenPort = settings.getInt(SK_OPENVPN_USER_LISTEN_PORT, 1194);
        listenProtocol = settings.getEnum(
                SK_OPENVPN_USER_LISTEN_PROTOCOL,
                TransportProtocol.UDP
        );
        mtuTest = settings.getBoolean(SK_OPENVPN_USER_MTU_TEST, true);
        remote = settings.get(SK_OPENVPN_USER_REMOTE, NetUtils.myHostname());
        deviceType = settings.get(SK_OPENVPN_USER_DEVICE_TYPE, "tun");
        deviceName = settings.get(SK_OPENVPN_USER_DEVICE_NAME, "arachne");
        clientNetwork = settings.get(SK_OPENVPN_USER_CLIENT_NETWORK, "192.168.100.0");
        clientMask = settings.getInt(SK_OPENVPN_USER_CLIENT_MASK, 24);
        keepaliveInterval = settings.getInt(SK_OPENVPN_USER_KEEPALIVE_INTERVAL, 10);
        keepaliveTimeout = settings.getInt(SK_OPENVPN_USER_KEEPALIVE_TIMEOUT, 60);
        pushDnsServers = settings.getList(SK_OPENVPN_USER_PUSH_DNS, NetUtils.getDnsServers());
        dnsSearch = settings.getList(
                SK_OPENVPN_USER_DNS_SEARCH,
                Arrays.asList(NetUtils.myDomain())
        );
        pushRoutes = settings.getList(SK_OPENVPN_USER_PUSH_ROUTES, NetUtils.getDefaultPushRoutes());
        internetThrouphVpn = settings.getBoolean(SK_OPENVPN_USER_INTERNET_THROUGH_VPN, false);
        authType = settings.getEnum(
                SK_OPENVPN_USER_AUTH_TYPE,
                AuthType.USERNAME_PASSWORD_CERTIFICATE
        );
        passwordVerificationType = settings.getEnum(
                SK_OPENVPN_USER_PWD_VERF_TYPE,
                PasswordVerificationType.HTTP_URL
        );
        authPamService = settings.get(SK_OPENVPN_USER_AUTH_PAM_SERVICE, "openvpn");
        authHttpUrl = settings.get(
                SK_OPENVPN_USER_AUTH_HTTP_URL,
                defaultAuthUrl(settings.getServerProperties())
        );
    }

    private String vpnName;
    private String clientConfigName;
    private String listenIp;
    private int listenPort;
    private TransportProtocol listenProtocol;
    private Boolean mtuTest;
    private String remote;
    private String deviceType;
    private String deviceName;
    private String clientNetwork;
    private int clientMask;
    private int keepaliveTimeout;
    private int keepaliveInterval;
    private List<String> pushDnsServers;
    private List<String> dnsSearch;
    private List<String> pushRoutes = new LinkedList<>();
    private Boolean internetThrouphVpn;
    private AuthType authType;
    private PasswordVerificationType passwordVerificationType;
    private String authPamService;
    private String authHttpUrl;

    public void save(Settings settings) {
        settings.put(SK_OPENVPN_USER_NAME, vpnName);
        settings.put(SK_OPENVPN_USER_CLIENT_CFG_NAME, clientConfigName);
        settings.put(SK_OPENVPN_USER_LISTEN_IP, listenIp);
        settings.put(SK_OPENVPN_USER_LISTEN_PORT, listenPort);
        settings.put(SK_OPENVPN_USER_LISTEN_PROTOCOL, listenProtocol.name());
        settings.put(SK_OPENVPN_USER_MTU_TEST, mtuTest);
        settings.put(SK_OPENVPN_USER_REMOTE, remote);
        settings.put(SK_OPENVPN_USER_DEVICE_TYPE, deviceType);
        settings.put(SK_OPENVPN_USER_DEVICE_NAME, deviceName);
        settings.put(SK_OPENVPN_USER_CLIENT_NETWORK, clientNetwork);
        settings.put(SK_OPENVPN_USER_CLIENT_MASK, clientMask);
        settings.put(SK_OPENVPN_USER_KEEPALIVE_INTERVAL, keepaliveInterval);
        settings.put(SK_OPENVPN_USER_KEEPALIVE_TIMEOUT, keepaliveTimeout);
        settings.put(SK_OPENVPN_USER_PUSH_DNS, pushDnsServers);
        settings.put(SK_OPENVPN_USER_DNS_SEARCH, dnsSearch);
        settings.put(SK_OPENVPN_USER_PUSH_ROUTES, pushRoutes);
        settings.put(SK_OPENVPN_USER_INTERNET_THROUGH_VPN, internetThrouphVpn);
        settings.put(SK_OPENVPN_USER_AUTH_TYPE, authType.name());
        settings.put(SK_OPENVPN_USER_PWD_VERF_TYPE, passwordVerificationType.name());
        settings.put(SK_OPENVPN_USER_AUTH_PAM_SERVICE, authPamService);
        settings.put(SK_OPENVPN_USER_AUTH_HTTP_URL, authHttpUrl);
    }

    public void setPushDnsServers(List<String> pushDnsServers) {
        logger.info(pushDnsServers.toString());
        this.pushDnsServers = new LinkedList<>(pushDnsServers);
        logger.info(this.pushDnsServers.toString());
    }

    private String defaultAuthUrl(ServerProperties serverProperties) {
        //String host = serverProperties.getAddress().getHostName();
        //int port = serverProperties.getPort();
        return "http://%s:%d/arachne".formatted(NetUtils.myHostname(), 8080);
    }

    public String getFormattedClientConfigName(String username) {
        return getVpnName()
                .replaceAll("%h", getRemote())
                .replaceAll("%u", username);
    }
}