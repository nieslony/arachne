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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class OpenVpnSiteSettings extends AbstractSettingsGroup {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnSiteSettings.class);

    @Getter
    @Setter
    static public class VpnSite extends AbstractSettingsGroup {

        private int id;
        private String name;
        private String description;
        private String remoteIp;
        private String sshUser;
        private String sshPrivateKey;
        private String preSharedkey;

        VpnSite(String name, String description, int id) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public VpnSite(Settings settings, int id) throws SettingsException {
            this.id = id;
            load(settings);
        }

        @Override
        protected String groupName() {
            return "%s.%d".formatted(super.groupName(), id);
        }

        @Override
        public String toString() {
            return description == null || description.isEmpty()
                    ? name
                    : name + " - " + description;
        }
    }

    private String listenIp = "0.0.0.0";
    private int listenPort = 1194;
    private TransportProtocol listenProtocol = TransportProtocol.UDP;
    private Boolean mtuTest = true;
    private String remote = NetUtils.myHostname();
    private String deviceType = "tun";
    private String deviceName = "arachne-site";
    private String clientNetwork = "192.168.101.0";
    private Integer clientMask = 24;
    private Integer keepaliveTimeout = 10;
    private Integer keepaliveInterval = 60;
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

    public VpnSite addSite(String name, String description) {
        logger.info("Adding site " + name);
        int id = vpnSiteIds.stream().max(Integer::compare).orElse(-1) + 1;
        VpnSite site = new VpnSite(name, description, id);
        vpnSiteIds.add(id);
        sites.put(id, site);
        return site;
    }

    public void deleteSite(Settings settings, int id) {
        logger.info("Removing site %d" + id);
        VpnSite site = sites.remove(id);
        site.delete(settings);
        vpnSiteIds.remove(id);
    }
}
