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
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.utils.net.NetUtils;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 *
 * @author claas
 */
@Controller
@Slf4j
public class FirewallController {

    @Autowired
    FirewallRuleRepository firewallRuleRepository;

    @Autowired
    FolderFactory folderFactory;

    @Autowired
    Settings settings;

    private List<String> buildIpSet(List<FirewallWhere> wheres, OpenVpnSettings openVpnSettings) {
        log.debug("Processing wheres" + wheres.toString());
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

        return NetUtils
                .filterSubnets(ret)
                .stream()
                .sorted()
                .toList();
    }

    public void writeRules(String fileName, FirewallRuleModel.VpnType vpnType)
            throws IOException, JSONException {
        OpenVpnUserSettings openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);

        JSONArray allRules = new JSONArray();
        for (var rule : firewallRuleRepository.findAllByVpnType(vpnType)) {
            if (!rule.isEnabled()) {
                continue;
            }
            List<String> sources;
            List<String> destination;
            if (rule.getRuleDirection() == FirewallRuleModel.RuleDirection.INCOMING) {
                sources = new LinkedList<>();
                destination = buildIpSet(rule.getTo(), openVpnUserSettings);
            } else {
                sources = buildIpSet(rule.getFrom(), openVpnUserSettings);
                destination = new LinkedList<>();
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
                                        what.getPortProtocol().toString()
                                )
                        );
                    }
                    case PortRange -> {
                        ports.add("%d-%d/%s"
                                .formatted(
                                        what.getPortFrom(),
                                        what.getPortTo(),
                                        what.getPortRangeProtocol()
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
            allRules.put(jRule);
        } // foreach rule

        try (FileWriter fileWriter = new FileWriter(
                folderFactory.getVpnConfigDir(fileName))) {
            fileWriter.write(allRules.toString(2) + "\n");
        }
    }
}
