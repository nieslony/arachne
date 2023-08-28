/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class OpenVpnSiteSettings {

    @Getter
    @Setter
    public class VpnSite {

        private int id;
        private String name;
        private String remoteIp;
        private String sshUser;
        private String sshPrivateKey;
        private String preSharedkey;

        private VpnSite(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private String listenIp;
    private int listenPort;
    private TransportProtocol listenProtocol;
    private Boolean mtuTest;
    private String remote;
    private String deviceType;
    private String deviceName;
    private String clientNetwork;
    private Integer clientMask;
    private Integer keepaliveTimeout;
    private Integer keepaliveInterval;

    private List<VpnSite> vpnSites;

    private static final String SK_OPENVPN_SITE_LISTEN_IP = "openvpn.site.listen-ip";
    private static final String SK_OPENVPN_SITE_LISTEN_PORT = "openvpn.site.listen-port";
    private static final String SK_OPENVPN_SITE_LISTEN_PROTOCOL = "openvpn.site.listen-protocol";
    private static final String SK_OPENVPN_SITE_MTU_TEST = "openvpn.site.mtu-test";
    private static final String SK_OPENVPN_SITE_REMOTE = "openvpn.site.remote";
    private static final String SK_OPENVPN_SITE_DEVICE_TYPE = "openvpn.site.deviceType";
    private static final String SK_OPENVPN_SITE_DEVICE_NAME = "openvpn.site.deviceName";
    private static final String SK_OPENVPN_SITE_CLIENT_NETWORK = "openvpn.site.clientNetwork";
    private static final String SK_OPENVPN_SITE_CLIENT_MASK = "openvpn.site.clientMask";
    private static final String SK_OPENVPN_SITE_KEEPALIVE_INTERVAL = "openvpn.site.keepaliveInterval";
    private static final String SK_OPENVPN_SITE_KEEPALIVE_TIMEOUT = "openvpn.site.keepaliveTimeout";

    public OpenVpnSiteSettings() {
    }

    public OpenVpnSiteSettings(Settings settings) {
        listenIp = settings.get(SK_OPENVPN_SITE_LISTEN_IP, "0.0.0.0");
        listenPort = settings.getInt(SK_OPENVPN_SITE_LISTEN_PORT, 1194);
        listenProtocol = settings.getEnum(SK_OPENVPN_SITE_LISTEN_PROTOCOL, TransportProtocol.UDP);
        mtuTest = settings.getBoolean(SK_OPENVPN_SITE_MTU_TEST, true);
        remote = settings.get(SK_OPENVPN_SITE_REMOTE, NetUtils.myHostname());
        deviceType = settings.get(SK_OPENVPN_SITE_DEVICE_TYPE, "tun");
        deviceName = settings.get(SK_OPENVPN_SITE_DEVICE_NAME, "arachne-site");
        clientNetwork = settings.get(SK_OPENVPN_SITE_CLIENT_NETWORK, "192.168.100.0");
        clientMask = settings.getInt(SK_OPENVPN_SITE_CLIENT_MASK, 24);
        keepaliveInterval = settings.getInt(SK_OPENVPN_SITE_KEEPALIVE_INTERVAL, 10);
        keepaliveTimeout = settings.getInt(SK_OPENVPN_SITE_KEEPALIVE_TIMEOUT, 60);
    }

    public void save(Settings settings) {
        settings.put(SK_OPENVPN_SITE_LISTEN_IP, listenIp);
        settings.put(SK_OPENVPN_SITE_LISTEN_PORT, listenPort);
        settings.put(SK_OPENVPN_SITE_LISTEN_PROTOCOL, listenProtocol);
        settings.put(SK_OPENVPN_SITE_MTU_TEST, mtuTest);
        settings.put(SK_OPENVPN_SITE_REMOTE, remote);
        settings.put(SK_OPENVPN_SITE_DEVICE_TYPE, deviceType);
        settings.put(SK_OPENVPN_SITE_DEVICE_NAME, deviceName);
        settings.put(SK_OPENVPN_SITE_CLIENT_NETWORK, clientNetwork);
        settings.put(SK_OPENVPN_SITE_CLIENT_MASK, clientMask);
        settings.put(SK_OPENVPN_SITE_KEEPALIVE_INTERVAL, keepaliveInterval);
        settings.put(SK_OPENVPN_SITE_KEEPALIVE_TIMEOUT, keepaliveTimeout);
    }

    public VpnSite addVpnSite() {
        int maxId = vpnSites.isEmpty()
                ? 0
                : vpnSites
                        .stream()
                        .max(Comparator.comparing(VpnSite::getId))
                        .get()
                        .getId();
        VpnSite vpnSite = new VpnSite(maxId + 1);
        return vpnSite;
    }
}
