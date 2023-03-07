/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.settings.SettingsModel;
import at.nieslony.arachne.settings.SettingsRepository;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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

    public OpenVpnUserSettings(SettingsRepository settingsRepository) {
        Optional<SettingsModel> setting;

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_NAME);
        vpnName = setting.isPresent() ? setting.get().getContent() : "Arachne OpenVPN - %u@%h";

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_LISTEN_IP);
        listenIp = setting.isPresent() ? setting.get().getContent() : "0.0.0.0";

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_LISTEN_PORT);
        listenPort = setting.isPresent() ? setting.get().getIntContent() : 1194;

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_LISTEN_PROTOCOL);
        listenProtocol = setting.isPresent() ? setting.get().getContent() : "UDP";

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_REMOTE);
        remote = setting.isPresent() ? setting.get().getContent() : NetUtils.myHostname();

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_DEVICE_TYPE);
        deviceType = setting.isPresent() ? setting.get().getContent() : "tun";

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_DEVICE_NAME);
        deviceName = setting.isPresent() ? setting.get().getContent() : "arachne";

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_CLIENT_NETWORK);
        clientNetwork = setting.isPresent() ? setting.get().getContent() : "192.168.100.0";

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_CLIENT_MASK);
        clientMask = setting.isPresent() ? setting.get().getIntContent() : 24;

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_KEEPALIVE_INTERVAL);
        keepaliveInterval = setting.isPresent() ? setting.get().getIntContent() : 10;

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_KEEPALIVE_TIMEOUT);
        keepaliveTimeout = setting.isPresent() ? setting.get().getIntContent() : 60;

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_PUSH_DNS);
        pushDnsServers = setting.isPresent() ? setting.get().getListContent() : NetUtils.getDnsServers();

        setting = settingsRepository.findBySetting(SK_OPENVPN_USER_PUSH_ROUTES);
        pushRoutes = setting.isPresent() ? setting.get().getListContent() : NetUtils.getDefaultPushRoutes();
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

    void save(SettingsRepository settingsRepository) {
        logger.info("Writing openVPN user config");
        Optional<SettingsModel> osm;

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_NAME);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(vpnName));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_NAME, vpnName));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_LISTEN_IP);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(listenIp));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_LISTEN_IP, listenIp));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_LISTEN_PORT);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(listenPort));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_LISTEN_PORT, listenPort));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_LISTEN_PROTOCOL);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(listenProtocol));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_LISTEN_PROTOCOL, listenProtocol));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_REMOTE);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(remote));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_REMOTE, remote));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_DEVICE_TYPE);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(deviceType));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_DEVICE_TYPE, deviceType));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_DEVICE_NAME);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(deviceName));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_DEVICE_NAME, deviceName));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_CLIENT_NETWORK);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(clientNetwork));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_CLIENT_NETWORK, clientNetwork));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_CLIENT_MASK);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(clientMask));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_CLIENT_MASK, clientMask));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_KEEPALIVE_INTERVAL);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(keepaliveInterval));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_KEEPALIVE_INTERVAL, keepaliveInterval));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_KEEPALIVE_TIMEOUT);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(keepaliveTimeout));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_KEEPALIVE_TIMEOUT, keepaliveTimeout));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_PUSH_DNS);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(pushDnsServers));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_PUSH_DNS, pushDnsServers));
        }

        osm = settingsRepository
                .findBySetting(SK_OPENVPN_USER_PUSH_ROUTES);
        if (osm.isPresent()) {
            settingsRepository.save(osm.get().setContent(pushRoutes));
        } else {
            settingsRepository.save(new SettingsModel(SK_OPENVPN_USER_PUSH_ROUTES, pushRoutes));
        }
    }

    public void setPushDnsServers(List<String> pushDnsServers) {
        logger.info(pushDnsServers.toString());
        this.pushDnsServers = new LinkedList<>(pushDnsServers);
        logger.info(this.pushDnsServers.toString());
    }

}
