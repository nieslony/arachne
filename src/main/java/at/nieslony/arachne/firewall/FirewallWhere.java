/*
 * Copyright (C) 2023 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.utils.NetUtils;
import at.nieslony.arachne.utils.SrvRecord;
import at.nieslony.arachne.utils.TransportProtocol;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Getter
@Setter
@Entity
@Table(name = "firewallWhere")
public class FirewallWhere {

    private static final Logger logger = LoggerFactory.getLogger(FirewallWhere.class);

    public enum Type {
        Hostname("Hostname"),
        Subnet("Subnet"),
        ServiceRecord("Service Record"),
        PushedDnsServers("Pushed DNS Servers");

        private final String label;

        private Type(String l) {
            label = l;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @Cascade(CascadeType.SAVE_UPDATE)
    @JoinColumn(name = "firewallRules_id")
    private FirewallRuleModel firewallRule;

    private Type type = Type.Hostname;

    private String hostname;

    private String subnet;
    private int subnetMask;

    private TransportProtocol serviceRecProtocol;
    private String serviceRecName;
    private String servicerecDomain;

    @Override
    public String toString() {
        return switch (type) {
            case Hostname ->
                hostname;
            case Subnet ->
                "%s/%d".formatted(subnet, subnetMask);
            case ServiceRecord ->
                "_%s._%s.%s"
                .formatted(
                serviceRecName,
                serviceRecProtocol.name().toLowerCase().toLowerCase(),
                servicerecDomain
                )
                .toLowerCase();
            case PushedDnsServers ->
                "Pushed DNS Servers";
        };
    }

    public List<String> resolve(OpenVpnUserSettings openvpnSettings) {
        List<String> addresses = new LinkedList<>();

        switch (type) {
            case Hostname -> {
                try {
                    InetAddress[] inetAddresses = InetAddress.getAllByName(hostname);
                    for (InetAddress addr : inetAddresses) {
                        if (addr instanceof Inet4Address) {
                            addresses.add(addr.getHostAddress());
                        }
                    }
                } catch (UnknownHostException ex) {
                    logger.error(
                            "Cannot resolve %s: %s"
                                    .formatted(hostname, ex.getMessage())
                    );
                }
            }
            case Subnet -> {
                String addr = "%s/%d".formatted(subnet, subnetMask);
                addresses.add(addr);
            }
            case ServiceRecord -> {
                try {
                    List<SrvRecord> srvRecs = NetUtils.srvLookup(
                            getServiceRecName(),
                            getServiceRecProtocol(),
                            getServicerecDomain()
                    );
                    for (SrvRecord srvRec : srvRecs) {
                        InetAddress[] addrs = InetAddress.getAllByName(srvRec.getHostname());
                        for (InetAddress addr : addrs) {
                            if (addr instanceof Inet4Address) {
                                addresses.add(addr.getHostAddress());
                            }
                        }
                    }
                } catch (NamingException | UnknownHostException ex) {
                    logger.info(
                            "Cannot find service record _%s._%s.%s: %s"
                                    .formatted(
                                            serviceRecName,
                                            serviceRecProtocol.toString(),
                                            servicerecDomain,
                                            ex.getMessage()
                                    )
                    );
                }
            }
            case PushedDnsServers -> {
                addresses.addAll(openvpnSettings.getPushDnsServers());
            }
        }

        return addresses;
    }
}
