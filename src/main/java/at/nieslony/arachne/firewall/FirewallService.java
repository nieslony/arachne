/*
 * Copyright (C) 2025 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.openvpn.OpenVpnSettings;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.openvpn.management.ManagementException;
import at.nieslony.arachne.openvpn.management.OpenVpnManagementService;
import at.nieslony.arachne.openvpn.management.commands.Status;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.usermatcher.UserMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.users.UserRepository;
import at.nieslony.arachne.utils.net.NetUtils;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author claas
 */
@Service
@Slf4j
public class FirewallService {

    @Autowired
    private FirewallRuleRepository firewallRuleRepository;

    @Autowired
    private OpenVpnManagementService openVpnManagementService;

    @Autowired
    private Settings settings;

    @Autowired
    private UserMatcherCollector userMatcherCollector;

    @Autowired
    private UserRepository userRepository;

    public FirewallService() throws NoSuchAlgorithmException {
    }

    private List<String> buildIpSet(List<FirewallWhere> wheres, OpenVpnSettings openVpnSettings) {
        log.debug("Building IP set from wheres" + wheres.toString());
        if (wheres.isEmpty()) {
            return new LinkedList<>();
        }
        if (wheres.get(0).getType() == FirewallWhere.Type.Everywhere) {
            return null;
        }
        List<String> ret = new LinkedList<>();
        wheres.forEach(where -> {
            if (where.getType() == FirewallWhere.Type.Subnet) {
                ret.add(where.toString());
            } else {
                ret.addAll(where.resolve(openVpnSettings));
            }
        });

        var sorted = NetUtils
                .filterSubnets(ret)
                .stream()
                .sorted()
                .distinct()
                .toList();
        log.debug("Built IP set: " + sorted.toString());
        return sorted;
    }

    private List<String> buildIpSet(List<FirewallWho> whos) {
        if (whos.size() == 1 && whos.get(0).isEverybody()) {
            return null;
        } else {
            return new LinkedList<>();
        }
    }

    public void updateWhos(String fileName)
            throws IOException, ManagementException {
        JSONArray incoming = new JSONArray();
        JSONArray outgoing = new JSONArray();
        Status.StatusInfo status = openVpnManagementService.getUserManagement().status();
        for (var r : firewallRuleRepository
                .findAllByVpnType(FirewallRuleModel.VpnType.USER)) {
            if (!r.isEnabled()
                    || r.getWho().isEmpty()
                    || r.getWho().getFirst().isEverybody()) {
                continue;
            }
            log.info("Updating rule " + r.toString());

            JSONObject jRule = new JSONObject();
            jRule.put("id", r.getId());

            log.debug("Current connections: " + status.connectionStatus());
            JSONArray userIps = new JSONArray();
            for (Status.ConnectionStatus cs : status.connectionStatus()) {
                log.info("Updating %s's with IP %s IP sets"
                        .formatted(cs.username(), cs.virtualAddress())
                );
                var user = userRepository.findByUsername(cs.username());

                for (FirewallWho who : r.getWho()) {
                    UserMatcher matcher = userMatcherCollector.buildUserMatcher(
                            who.getUserMatcherClassName(),
                            who.getParameter()
                    );
                    if (matcher.isUserMatching(user)) {
                        log.debug("Rule %s matches".formatted(matcher.toString()));
                        userIps.put(cs.virtualAddress().getHostAddress());
                        break;
                    } else {
                        log.debug("Rule %s does not match".formatted(matcher.toString()));
                    }
                }
                switch (r.getRuleDirection()) {
                    case FirewallRuleModel.RuleDirection.INCOMING -> {
                        jRule.put("source", userIps);
                        incoming.put(jRule);
                    }
                    case FirewallRuleModel.RuleDirection.OUTGOING -> {
                        jRule.put("destination", userIps);
                        outgoing.put(jRule);
                    }
                }

            }
        }

        JSONObject jRules = new JSONObject();
        jRules.put("incoming", incoming);
        jRules.put("outgoing", outgoing);
        String rulesStr = jRules.toString(2) + "\n";

        Files.deleteIfExists(Path.of(fileName));
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            log.info("Writing " + fileName);
            fileWriter.write(rulesStr);
            fileWriter.close();
        }
    }

    public void writeRulesUpdates(
            String fileName,
            FirewallRuleModel.VpnType vpnType,
            FirewallRuleModel.RuleDirection direction,
            EditFirewallRule.Changes changes
    ) throws IOException, JSONException {
        /*
        {
            incoming:
                1: {
                    "destination": []
                    "who": []
                }

            "incoming": [
                {
                    "id": 1,
                    "source": []
                    "destination": []
                ]
            ],
        }
         */
        OpenVpnUserSettings openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);
        JSONArray jRules = new JSONArray();

        for (var r : firewallRuleRepository
                .findAllByVpnTypeAndRuleDirection(vpnType, direction)) {
            if (r.isEnabled()) {
                JSONObject jRule = new JSONObject();
                jRule.put("id", r.getId());
                boolean ignoreToWho
                        = r.getVpnType() == FirewallRuleModel.VpnType.USER
                        && r.getRuleDirection() == FirewallRuleModel.RuleDirection.OUTGOING;
                if (!r.isToEveryWhere() && changes.isToChanged() && !ignoreToWho) {
                    jRule.put("destination",
                            buildIpSet(
                                    r.getTo(),
                                    openVpnUserSettings
                            )
                    );
                }
                boolean ignoreFromWho
                        = r.getVpnType() == FirewallRuleModel.VpnType.USER
                        && r.getRuleDirection() == FirewallRuleModel.RuleDirection.INCOMING;
                if (!r.isFromEveryWhere() && changes.isFromChanged() && !ignoreFromWho) {
                    jRule.put("source",
                            buildIpSet(
                                    r.getFrom(),
                                    openVpnUserSettings
                            )
                    );
                }
                jRules.put(jRule);
            }
        }

        JSONObject jContent = new JSONObject();
        jContent.put(direction.name().toLowerCase(), jRules);
        String rulesStr = jContent.toString(2) + "\n";
        Files.deleteIfExists(Path.of(fileName));
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            log.info("Writing " + fileName);
            fileWriter.write(rulesStr);
            fileWriter.close();
        }
    }

    public void writeRules(String fileName, FirewallRuleModel.VpnType vpnType)
            throws IOException, JSONException {
        OpenVpnUserSettings openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);

        JSONArray incomingRules = new JSONArray();
        JSONArray outgoingRules = new JSONArray();
        for (var rule : firewallRuleRepository.findAllByVpnType(vpnType)) {
            log.debug("Processing rule " + rule.toString());
            if (!rule.isEnabled()) {
                continue;
            }

            List<String> sources;
            List<String> destination;
            if (rule.getRuleDirection() == FirewallRuleModel.RuleDirection.INCOMING) {
                sources = buildIpSet(rule.getWho());
                destination = buildIpSet(rule.getTo(), openVpnUserSettings);
            } else {
                sources = buildIpSet(rule.getFrom(), openVpnUserSettings);
                destination = buildIpSet(rule.getWho());
            }

            Set<String> ports = new TreeSet<>();
            Set<String> services = new TreeSet<>();
            for (var what : rule.getWhat()) {
                switch (what.getType()) {
                    case Everything -> {
                    }
                    case OnePort -> {
                        ports.add("%d/%s"
                                .formatted(
                                        what.getPort(),
                                        what.getPortProtocol()
                                                .toString()
                                                .toLowerCase()
                                )
                        );
                    }
                    case PortRange -> {
                        ports.add("%d-%d/%s"
                                .formatted(
                                        what.getPortFrom(),
                                        what.getPortTo(),
                                        what.getPortRangeProtocol()
                                                .toString()
                                                .toLowerCase()
                                )
                        );
                    }
                    case Service -> {
                        FirewalldService service = FirewalldService
                                .getService(what.getService());
                        if (service.getIncludes().isEmpty()) {
                            services.add(service.getName());
                        } else {
                            services.addAll(service.getIncludes());
                        }
                    }
                }
            }
            JSONObject jRule = new JSONObject();
            jRule.put("id", rule.getId());
            if (sources != null) {
                jRule.put("sources", new JSONArray(sources));
            }
            if (destination != null) {
                jRule.put("destination", new JSONArray(destination));
            }
            if (!ports.isEmpty()) {
                jRule.put("ports", new JSONArray(ports));
            }
            if (!services.isEmpty()) {
                jRule.put("services", new JSONArray(services));
            }
            if (rule.getRuleDirection() == FirewallRuleModel.RuleDirection.INCOMING) {
                incomingRules.put(jRule);
            } else {
                outgoingRules.put(jRule);
            }
        } // foreach rule
        JSONObject allRules = new JSONObject();
        allRules.put("incoming", incomingRules);
        allRules.put("outgoing", outgoingRules);

        switch (vpnType) {
            case FirewallRuleModel.VpnType.USER -> {
                UserFirewallBasicsSettings basicSettings
                        = settings.getSettings(UserFirewallBasicsSettings.class);
                allRules.put("icmp-rules", basicSettings.getIcmpRules().name());
            }
            case FirewallRuleModel.VpnType.SITE -> {
                SiteFirewallBasicsSettings basicSettings
                        = settings.getSettings(SiteFirewallBasicsSettings.class);
                allRules.put("icmp-rules", basicSettings.getIcmpRules().name());
            }

        }
        String rulesStr = allRules.toString(2) + "\n";

        Files.deleteIfExists(Path.of(fileName));
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(rulesStr);
        }
    }
}
