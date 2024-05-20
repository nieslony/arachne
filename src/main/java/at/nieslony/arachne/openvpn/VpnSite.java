/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.utils.net.NetUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "vpn-sites")
public class VpnSite {

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

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    // Basics
    @Column(nullable = false, unique = true)
    private String name;
    private String description;
    @Builder.Default
    private boolean defaultSite = false;
    // Connection
    @Builder.Default
    private String remoteHost = "www.example.com";
    @Builder.Default
    private SiteVerification siteVerification = SiteVerification.DNS;
    @Builder.Default
    private List<String> ipWhiteList = new LinkedList<>();
    // DNS
    @Builder.Default
    private boolean inheritDnsServers = true;
    @Builder.Default
    private List<String> pushDnsServers = NetUtils.getDnsServers();
    @Builder.Default
    private boolean inheritPushDomains = true;
    @Builder.Default
    private List<String> pushSearchDomains = Arrays.asList(NetUtils.myDomain());
    // Routing
    @Builder.Default
    private boolean inheritPushRoutes = true;
    @Builder.Default
    private List<String> pushRoutes = NetUtils.getDefaultPushRoutes();
    @Builder.Default
    boolean inheritRouteInternetThroughVpn = true;
    @Builder.Default
    private boolean routeInternetThroughVpn = false;
    private String networkManagerConnectionUuid;

    public VpnSite() {
    }

    public void updateInheritedValues(VpnSite copyFrom) {
        if (!defaultSite && copyFrom != null) {
            if (inheritDnsServers) {
                pushDnsServers = copyFrom.getPushDnsServers();
            }
            if (inheritPushDomains) {
                pushSearchDomains = copyFrom.getPushSearchDomains();
            }
            if (inheritPushRoutes) {
                pushRoutes = copyFrom.getPushRoutes();
            }
            if (inheritRouteInternetThroughVpn) {
                routeInternetThroughVpn = copyFrom.isRouteInternetThroughVpn();
            }
        }
    }

    public String label() {
        return description == null || description.isEmpty() ? name : name + " - " + description;
    }
}
