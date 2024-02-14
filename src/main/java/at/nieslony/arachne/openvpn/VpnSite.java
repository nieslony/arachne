/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.net.NetUtils;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@ToString
public class VpnSite extends AbstractSettingsGroup {

    public enum SiteVerification {
        NONE("No Verification"),
        DNS("Hostname matches DNS A-record"),
        WHITELIST("IP in Whitelist");

        private SiteVerification(String label) {
            this.label = label;
        }

        private final String label;

        @Override
        public String toString() {
            return label;
        }
    }

    private Integer id;
    private String name;
    private String description;
    private String remoteHost;
    @Builder.Default
    private SiteVerification siteVerification = SiteVerification.DNS;
    @Builder.Default
    private List<String> ipWhiteList = new LinkedList<>();
    private String sshUser;
    private String sshPrivateKey;
    @Builder.Default
    private boolean inheritDnsServers = true;
    @Builder.Default
    private List<String> pushDnsServers = NetUtils.getDnsServers();
    @Builder.Default
    private boolean inheritPushDomains = true;
    @Builder.Default
    private List<String> pushSearchDomains = Arrays.asList(NetUtils.myDomain());

    public List<String> getPushDnsServers(VpnSite defaultSite) {
        return inheritDnsServers
                ? defaultSite.getPushDnsServers()
                : getPushDnsServers();
    }

    public List<String> getPushSearchDomains(VpnSite defaultSite) {
        return inheritPushDomains
                ? defaultSite.getPushSearchDomains()
                : getPushSearchDomains();
    }

    @Builder.Default
    private boolean inheritPushRoutes = true;
    @Builder.Default
    private List<String> pushRoutes = NetUtils.getDefaultPushRoutes();
    @Builder.Default
    boolean inheritRouteInternetThroughVpn = true;
    @Builder.Default
    private boolean routeInternetThroughVpn = false;

    public List<String> getPushRoutes(VpnSite defaultSite) {
        return inheritPushRoutes
                ? defaultSite.getPushRoutes()
                : getPushRoutes();
    }

    public VpnSite(Settings settings, int id) throws SettingsException {
        this.id = id;
        load(settings);
    }

    @Override
    protected String groupName() {
        return "%s.%d".formatted(super.groupName(), id);
    }

    public String label() {
        return description == null || description.isEmpty() ? name : name + " - " + description;
    }
}
