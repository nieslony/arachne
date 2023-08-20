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
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.usermatcher.EverybodyMatcher;
import at.nieslony.arachne.usermatcher.UserMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.security.RolesAllowed;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author claas
 */
@RestController
@RequestMapping("/api/firewall")
public class FirewallRestController {

    private static final Logger logger = LoggerFactory.getLogger(FirewallRestController.class);

    @Autowired
    private FirewallRuleRepository firewallRuleRepository;

    @Autowired
    private UserMatcherCollector userMatcherCollector;

    @Autowired
    private Settings settings;

    @Getter
    @Setter
    @EqualsAndHashCode
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class RichRule {

        private String destinationAddress;
        private String serviceName;
        private String port;
    }

    @GetMapping("/user_rules")
    @RolesAllowed(value = {"USER"})
    public List<RichRule> getUserRules(
            @RequestParam(required = false, name = "type") String type
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<RichRule> richRules = new LinkedList<>();

        logger.info("Get firewall rules for " + username);
        for (FirewallRuleModel rule : firewallRuleRepository.findAll()) {
            logger.info(rule.toString());
            if (!rule.isEnabled()) {
                logger.info("Ignoring disabled rule");
                continue;
            }
            boolean matches = false;
            for (FirewallWho who : rule.getWho()) {
                if (who.getUserMatcherClassName().equals(EverybodyMatcher.class.getName())) {
                    logger.info("Ignoring everybody rule");
                    break;
                }
                UserMatcher matcher = userMatcherCollector.buildUserMatcher(
                        who.getUserMatcherClassName(),
                        who.getParameter()
                );
                if (matcher.isUserMatching(username)) {
                    matches = true;
                    logger.info(
                            "Rule %s matches user %s"
                                    .formatted(rule.toString(), username)
                    );
                    break;
                }
                logger.info(
                        "Rule %s does not match user %s"
                                .formatted(rule.toString(), username)
                );
            }
            if (matches) {
                richRules.addAll(createRichRules(rule));
            }
        }

        return richRules;
    }

    @GetMapping("/everybody_rules")
    public FirewallEverybodyRules getEverybodyRules() {
        FirewallEverybodyRules firewallEveryBodyRules = new FirewallEverybodyRules();
        List<RichRule> richRules = new LinkedList<>();

        for (FirewallRuleModel rule : firewallRuleRepository.findAll()) {
            if (rule.isEnabled()) {
                for (FirewallWho who : rule.getWho()) {
                    if (who.getUserMatcherClassName().equals(EverybodyMatcher.class.getName())) {
                        richRules.addAll(createRichRules(rule));
                    }
                }
            }
        }
        firewallEveryBodyRules.setRichRules(richRules);

        FirewallBasicsSettings firewallBasicsSettings = new FirewallBasicsSettings(settings);
        firewallEveryBodyRules.setIcmpRules(firewallBasicsSettings.getIcmpRules());

        return firewallEveryBodyRules;
    }

    private List<RichRule> createRichRules(FirewallRuleModel rule) {
        List<RichRule> richRules = new LinkedList<>();
        OpenVpnUserSettings openvpnSettings = new OpenVpnUserSettings(settings);

        Set<String> addresses = new HashSet<>();
        for (FirewallWhere where : rule.getWhere()) {
            addresses.addAll(where.resolve(openvpnSettings));
        }

        for (String addr : addresses) {
            for (FirewallWhat what : rule.getWhat()) {
                switch (what.getType()) {
                    case OnePort -> {
                        RichRule richRule = new RichRule();
                        richRule.setDestinationAddress(addr);
                        String portString = "port=\"%d\" protocol=\"%s\""
                                .formatted(
                                        what.getPort(),
                                        what.getPortProtocol().toString().toLowerCase()
                                );
                        richRule.setPort(portString);
                        richRules.add(richRule);
                    }
                    case PortRange -> {
                        RichRule richRule = new RichRule();
                        richRule.setDestinationAddress(addr);
                        String portsString = "port=\"%d-%d\" protocol=\"%s\""
                                .formatted(
                                        what.getPortFrom(),
                                        what.getPortTo(),
                                        what.getPortRangeProtocol().toString().toLowerCase()
                                );
                        richRule.setPort(portsString);
                        richRules.add(richRule);
                    }
                    case Service -> {
                        var services = FirewalldService.getServiceRecursive(what.getService());
                        for (FirewalldService service : services) {
                            RichRule richRule = new RichRule();
                            richRule.setDestinationAddress(addr);
                            richRule.setServiceName(service.getName());
                            richRules.add(richRule);
                        }
                    }
                    case Everything -> {
                    }
                }
            }
        }

        return richRules;
    }
}
