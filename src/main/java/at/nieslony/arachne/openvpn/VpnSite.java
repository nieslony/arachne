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
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
public class VpnSite extends AbstractSettingsGroup {

    private Integer id;
    private String name;
    private String description;
    private String remoteHost;
    private String sshUser;
    private String sshPrivateKey;
    private String preSharedKey;

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
        return description == null || description.isEmpty() ? name : name + " - " + description;
    }
}
