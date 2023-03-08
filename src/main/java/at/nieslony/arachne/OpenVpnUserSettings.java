/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.settings.Settings;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Data
@ToString
public class OpenVpnUserSettings {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnUserSettings.class);

    private static final String SK_OPENVPN_USER_NAME = "openvpn.user.name";
    private static final String SK_OPENVPN_USER_LISTEN_IP = "openvpn.user.listenIp";
    private static final String SK_OPENVPN_USER_LISTEN_PORT = "openvpn.user.listenPort";
    private static final String SK_OPENVPN_USER_LISTEN_PROTOCOL = "openvpn.user.listenProtocol";
    private static final String SK_OPENVPN_USER_REMOTE = "openvpn.user.remote";
    private static final String SK_OPENVPN_USER_DEVICE_TYPE = "openvpn.user.deviceType";
    private static final String SK_OPENVPN_USER_DEVICE_NAME = "openvpn.user.deviceName";
    private static final String SK_OPENVPN_USER_CLIENT_NETWORK = "openvpn.user.clientNetwork";
    private static final String SK_OPENVPN_USER_CLIENT_MASK = "openvpn.user.clientMask";
    private static final String SK_OPENVPN_USER_KEEPALIVE_INTERVAL = "openvpn.user.keepaliveInterval";
    private static final String SK_OPENVPN_USER_KEEPALIVE_TIMEOUT = "openvpn.user.keepaliveTimeout";
    private static final String SK_OPENVPN_USER_PUSH_DNS = "openvpn.user.pushdns";
    private static final String SK_OPENVPN_USER_PUSH_ROUTES = "openvpn.user.pushroutes";

    public OpenVpnUserSettings() {
    }

    public OpenVpnUserSettings(Settings settings) {
        vpnName = settings.get(SK_OPENVPN_USER_NAME, "Arachne OpenVPN - %u@%h");
        listenIp = settings.get(SK_OPENVPN_USER_LISTEN_IP, "0.0.0.0");
        listenPort = settings.getInt(SK_OPENVPN_USER_LISTEN_PORT, 1194);
        listenProtocol = settings.get(SK_OPENVPN_USER_LISTEN_PROTOCOL, "UDP");
        remote = settings.get(SK_OPENVPN_USER_REMOTE, NetUtils.myHostname());
        deviceType = settings.get(SK_OPENVPN_USER_DEVICE_TYPE, "tun");
        deviceName = settings.get(SK_OPENVPN_USER_DEVICE_NAME, "arachne");
        clientNetwork = settings.get(SK_OPENVPN_USER_CLIENT_NETWORK, "192.168.100.0");
        clientMask = settings.getInt(SK_OPENVPN_USER_CLIENT_MASK, 24);
        keepaliveInterval = settings.getInt(SK_OPENVPN_USER_KEEPALIVE_INTERVAL, 10);
        keepaliveTimeout = settings.getInt(SK_OPENVPN_USER_KEEPALIVE_TIMEOUT, 60);
        pushDnsServers = settings.getList(SK_OPENVPN_USER_PUSH_DNS, NetUtils.getDnsServers());
        pushRoutes = settings.getList(SK_OPENVPN_USER_PUSH_ROUTES, NetUtils.getDefaultPushRoutes());
    }

    private String vpnName;
    private String listenIp;
    private int listenPort;
    private String listenProtocol;
    private String remote;
    private String deviceType;
    private String deviceName;
    private String clientNetwork;
    private int clientMask;
    private int keepaliveTimeout;
    private int keepaliveInterval;
    private List<String> pushDnsServers;
    private List<String> pushRoutes = new LinkedList<>();

    void save(Settings settings) {
        settings.put(SK_OPENVPN_USER_NAME, vpnName);
        settings.put(SK_OPENVPN_USER_LISTEN_IP, listenIp);
        settings.put(SK_OPENVPN_USER_LISTEN_PORT, listenPort);
        settings.put(SK_OPENVPN_USER_LISTEN_PROTOCOL, listenProtocol);
        settings.put(SK_OPENVPN_USER_REMOTE, remote);
        settings.put(SK_OPENVPN_USER_DEVICE_TYPE, deviceType);
        settings.put(SK_OPENVPN_USER_DEVICE_NAME, deviceName);
        settings.put(SK_OPENVPN_USER_CLIENT_NETWORK, clientNetwork);
        settings.put(SK_OPENVPN_USER_CLIENT_MASK, clientMask);
        settings.put(SK_OPENVPN_USER_KEEPALIVE_INTERVAL, keepaliveInterval);
        settings.put(SK_OPENVPN_USER_KEEPALIVE_TIMEOUT, keepaliveTimeout);
        settings.put(SK_OPENVPN_USER_PUSH_DNS, pushDnsServers);
        settings.put(SK_OPENVPN_USER_PUSH_ROUTES, pushRoutes);
    }

    public void setPushDnsServers(List<String> pushDnsServers) {
        logger.info(pushDnsServers.toString());
        this.pushDnsServers = new LinkedList<>(pushDnsServers);
        logger.info(this.pushDnsServers.toString());
    }

}
