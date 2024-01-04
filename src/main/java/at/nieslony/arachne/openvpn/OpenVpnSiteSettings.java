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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class OpenVpnSiteSettings extends AbstractSettingsGroup {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnSiteSettings.class);

    private String listenIp = "0.0.0.0";
    private int listenPort = 1194;
    private TransportProtocol listenProtocol = TransportProtocol.UDP;
    private Boolean mtuTest = true;
    private String remote = NetUtils.myHostname();
    private String deviceType = "tun";
    private String deviceName = "arachne-site";
    private String clientNetwork = "192.168.101.0";
    private Integer clientMask = 24;
    private Integer keepaliveTimeout = 60;
    private Integer keepaliveInterval = 10;
    private List<Integer> vpnSiteIds = new LinkedList<>();

    private final Map<Integer, VpnSite> sites = new HashMap<>();

    public OpenVpnSiteSettings() {
    }

    public VpnSite getVpnSite(int id) {
        return sites.get(id);
    }

    public Collection<VpnSite> getVpnSites() {
        return sites.values();
    }

    @Override
    public void load(Settings settings) throws SettingsException {
        super.load(settings);
        if (vpnSiteIds == null || vpnSiteIds.isEmpty()) {
            addSite("Default", "Default Settings for all Sites");
        } else {
            for (int id : vpnSiteIds) {
                VpnSite site = new VpnSite(settings, id);
                sites.put(id, site);
            }
        }
    }

    @Override
    public void save(Settings settings) throws SettingsException {
        super.save(settings);
        for (VpnSite site : sites.values()) {
            site.save(settings);
        }
    }

    @Transactional
    public VpnSite addSite(String name, String description) {
        logger.info("Adding site " + name);
        int id = vpnSiteIds.stream().max(Integer::compare).orElse(-1) + 1;
        VpnSite site = new VpnSite(name, description, id);
        vpnSiteIds.add(id);
        sites.put(id, site);
        return site;
    }

    @Transactional
    public void deleteSite(Settings settings, int id) {
        logger.info("Removing site %d".formatted(id));
        VpnSite site = sites.remove(id);
        site.delete(settings);
        vpnSiteIds.remove(id);
    }

    public static String createPreSharedKey() {
        SecureRandom secureRandom;
        try {
            secureRandom = SecureRandom.getInstance("NativePRNG");
        } catch (NoSuchAlgorithmException ex) {
            logger.error("Cannot create secure random: " + ex.getMessage());
            return null;
        }
        byte[] values = new byte[2048 / 8];
        secureRandom.nextBytes(values);
        String keyBase64 = String.join(
                "\n",
                Base64
                        .toBase64String(values)
                        .split("(?<=\\G.{78})")
        );

        return """
               #
               # 2048 bit OpenVPN static key
               #
               -----BEGIN OpenVPN Static key V1-----
               %s
               -----END OpenVPN Static key V1-----
               """.formatted(keyBase64);
    }
}
