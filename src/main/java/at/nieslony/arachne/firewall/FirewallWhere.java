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

import at.nieslony.arachne.openvpn.OpenVpnSettings;
import at.nieslony.arachne.utils.net.DnsServiceName;
import at.nieslony.arachne.utils.net.MxRecord;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.SrvRecord;
import at.nieslony.arachne.utils.net.TransportProtocol;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class FirewallWhere {

    private static final Logger logger = LoggerFactory.getLogger(FirewallWhere.class);

    public enum Type {
        Hostname("Hostname"),
        Subnet("Subnet"),
        ServiceRecord("Service Record"),
        MxRecord("MX (mail exchange) Record"),
        PushedDnsServers("Pushed DNS Servers"),
        Everywhere("Everywhere");

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
    @Cascade(CascadeType.ALL)
    @JoinColumn(name = "firewallRules_id")
    private FirewallRuleModel firewallRule;

    private Type type = Type.Hostname;

    private String hostname = NetUtils.myHostname();

    private String subnet = "0.0.0.0";
    private int subnetMask = 32;

    private TransportProtocol serviceRecProtocol = TransportProtocol.TCP;
    private String serviceRecName = DnsServiceName.getKnownServices()
            .keySet().toArray(new String[0])[0];
    private String serviceRecDomain = NetUtils.myDomain();

    private String mxDomain = NetUtils.myDomain();

    @Override
    public String toString() {
        return switch (type) {
            case Everywhere ->
                Type.Everywhere.label;
            case Hostname ->
                hostname;
            case Subnet ->
                "%s/%d".formatted(subnet, subnetMask);
            case ServiceRecord ->
                "_%s._%s.%s"
                .formatted(
                serviceRecName,
                serviceRecProtocol.name().toLowerCase().toLowerCase(),
                serviceRecDomain
                )
                .toLowerCase();
            case PushedDnsServers ->
                "Pushed DNS Servers";
            case MxRecord ->
                "MX record " + mxDomain;
        };
    }

    public List<String> resolve(OpenVpnSettings openvpnSettings) {
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
                            getServiceRecDomain()
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
                                            serviceRecDomain,
                                            ex.getMessage()
                                    )
                    );
                }
            }
            case PushedDnsServers -> {
                addresses.addAll(openvpnSettings.getPushDnsServers());
            }
            case MxRecord -> {
                try {
                    for (MxRecord mx : NetUtils.mxLookup(mxDomain)) {
                        addresses.add(mx.getValue());
                    }
                } catch (NamingException ex) {
                    logger.error("Cannot find MX records for domain %s: %s"
                            .formatted(mxDomain, ex.getMessage()));
                }
            }
        }

        return getWithIp(addresses);
    }

    private List<String> getWithIp(List<String> addresses) {
        Set<String> ips = new HashSet<>();

        for (String addr : addresses) {
            try {
                InetAddress[] as = InetAddress.getAllByName(addr);
                for (InetAddress a : as) {
                    if (a instanceof Inet4Address) {
                        ips.add(a.getHostAddress());
                    }
                }
            } catch (UnknownHostException ex) {
                logger.warn("Unknown host: %s, ignoring".formatted(addr));
            }
        }

        return new LinkedList<>(ips);
    }

    public Component createInfoPopover(Component parent) {
        Component info = switch (getType()) {
            case Everywhere ->
                null;
            case Hostname -> {
                try {
                    var addrs = InetAddress.getAllByName(getHostname());
                    if (addrs.length > 0) {
                        UnorderedList list = new UnorderedList();
                        for (var a : addrs) {
                            list.add(new ListItem(a.toString()));
                        }
                        yield list;
                    }
                    yield new Text("Unresolvable");
                } catch (UnknownHostException ex) {
                    yield null;
                }
            }
            case MxRecord -> {
                try {
                    UnorderedList list = new UnorderedList();
                    NetUtils.mxLookup(mxDomain).forEach(mx -> {
                        list.add(new ListItem(mx.getValue()));
                    });
                    yield list;
                } catch (NamingException ex) {
                    yield new Text("Unresolvable");
                }
            }
            case ServiceRecord -> {
                try {
                    List<SrvRecord> srvRecs = NetUtils.srvLookup(
                            getServiceRecName(),
                            getServiceRecProtocol(),
                            getServiceRecDomain()
                    );
                    UnorderedList list = new UnorderedList();
                    srvRecs.forEach(rec -> {
                        list.add(new ListItem(rec.getHostname()));
                    });
                    yield list;
                } catch (NamingException ex) {
                    yield new Text("Unresolvable");
                }
            }
            default ->
                null;
        };

        if (info != null) {
            Popover popover = new Popover(info);
            popover.setTarget(parent);
            popover.setPosition(PopoverPosition.END);
            popover.addThemeVariants(PopoverVariant.LUMO_ARROW);
            popover.setOpenOnClick(false);
            popover.setOpenOnHover(true);
            popover.setWidth("32em");
            return popover;
        } else {
            return null;
        }
    }

    public static FirewallWhere createEverywhere() {
        FirewallWhere where = new FirewallWhere();
        where.setType(FirewallWhere.Type.Everywhere);
        return where;
    }
}
