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
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
@Slf4j
public class OpenVpnSiteSettings extends AbstractSettingsGroup {

    private boolean alreadyConfigured = false;

    private String listenIp = "0.0.0.0";
    private int listenPort = 1194;
    private TransportProtocol listenProtocol = TransportProtocol.UDP;
    private Boolean mtuTest = true;
    private String connectToHost = NetUtils.myHostname();
    private String deviceType = "tun";
    private String deviceName = "arachne-site";
    private String siteNetwork = "192.168.130.0";
    private Integer siteNetworkMask = 24;
    private Integer keepaliveTimeout = 60;
    private Integer keepaliveInterval = 10;
    private List<Integer> vpnSiteIds = new LinkedList<>();

    private String runAsUser;

    @Override
    public void save(Settings settings) throws SettingsException {
        alreadyConfigured = true;
        super.save(settings);
    }
}
