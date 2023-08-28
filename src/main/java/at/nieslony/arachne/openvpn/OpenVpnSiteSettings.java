/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.utils.TransportProtocol;
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
    @ToString
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
    }

    private String listenIp;
    private int listenPort;
    private TransportProtocol listenProtocol;
    private boolean allowConfigureIpOnly;
    private List<VpnSite> vpnSites;

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
