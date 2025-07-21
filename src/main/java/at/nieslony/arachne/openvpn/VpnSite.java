/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.openvpn.vpnsite.RemoteNetwork;
import at.nieslony.arachne.openvpn.vpnsite.SiteVerification;
import at.nieslony.arachne.openvpn.vpnsite.UploadConfigType;
import at.nieslony.arachne.ssh.SshAuthType;
import static at.nieslony.arachne.ssh.SshAuthType.USERNAME_PASSWORD;
import at.nieslony.arachne.ssh.SshKeyEntity;
import at.nieslony.arachne.utils.net.NetUtils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.ObjectUtils;

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
public class VpnSite implements OpenVpnSettings {

    public enum ClientIpMode {
        AUTO("Auto"),
        FIXED_IP("Fixed IP"),
        BY_HOSTNAME("By Hostname");

        ClientIpMode(String label) {
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
    private String siteHostname = "www.example.com";
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

    @Builder.Default
    private String uploadToHost = null;

    @Builder.Default
    private String username = "";
    @Builder.Default
    private boolean sudoRequired = false;
    @Builder.Default
    private boolean restartOpenVpn = false;
    @Builder.Default
    private boolean enableOpenVpn = false;

    @Builder.Default
    private UploadConfigType uploadConfigType = UploadConfigType.NMCL;

    @Builder.Default
    private String connectionName = "OpenVPN_" + NetUtils.myHostname();
    @Builder.Default
    private String certitifaceFolder = "/etc/pki/arachne";
    @Builder.Default
    private boolean enableConnection = false;
    @Builder.Default
    private boolean autostartConnection = false;

    @Builder.Default
    private String destinationFolder = "/etc/openvpn/client";

    @Builder.Default
    private SshAuthType sshAuthType = USERNAME_PASSWORD;

    @ManyToOne
    @JoinColumn(name = "ssh-key_id")
    private SshKeyEntity sshKey;

    @Builder.Default
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    List<RemoteNetwork> remoteNetworks = new LinkedList<>();

    @Builder.Default
    private ClientIpMode clientIpMode = ClientIpMode.AUTO;

    @Builder.Default
    private String clientIp = "";

    @Builder.Default
    private String clientHostname = "";

    public VpnSite() {
    }

    public String getUploadToHost() {
        return ObjectUtils.isEmpty(uploadToHost) ? siteHostname : uploadToHost;
    }

    public void setUploadToHost(String uploadToHost) {
        this.uploadToHost = uploadToHost;
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
        return ObjectUtils.isEmpty(description) ? name : name + " - " + description;
    }
}
