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

import at.nieslony.arachne.apiindex.ShowApiDetails;
import at.nieslony.arachne.ldap.LdapUserSource;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.usermatcher.EverybodyMatcher;
import at.nieslony.arachne.usermatcher.UserMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.security.RolesAllowed;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpResponseException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
@Slf4j
public class FirewallRestController {

    @Autowired
    private FirewallRuleRepository firewallRuleRepository;

    @Autowired
    private UserMatcherCollector userMatcherCollector;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LdapUserSource ldapUserSource;

    @Autowired
    private Settings settings;

    @Autowired
    private FirewallController firewallController;

    @Getter
    @Setter
    @EqualsAndHashCode
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ShowApiDetails
    public class RichRule {

        private String destinationAddress;
        private String serviceName;
        private String port;
    }

    @Getter
    public class MatchingRules {

        final Set<Long> incoming = new TreeSet<>();
        final Set<Long> outgoing = new TreeSet<>();
    }

    @GetMapping("/matching_rules")
    @RolesAllowed(value = {"USER"})
    public MatchingRules getMatchingRules()
            throws HttpResponseException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserModel user = userRepository.findByUsername(username);
        if (user == null) {
            user = ldapUserSource.findUser(username);
        }
        if (user == null) {
            log.error("User %s doen't exist -> no fire wall rules".formatted(username));
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), "User not found");
        }

        MatchingRules matchingRules = new MatchingRules();
        for (FirewallRuleModel rule : firewallRuleRepository.findAllByVpnType(FirewallRuleModel.VpnType.USER)) {
            if (!rule.isEnabled()) {
                continue;
            }
            for (FirewallWho who : rule.getWho()) {
                if (who.getUserMatcherClassName().equals(EverybodyMatcher.class.getName())) {
                    break;
                }
                UserMatcher matcher = userMatcherCollector.buildUserMatcher(
                        who.getUserMatcherClassName(),
                        who.getParameter()
                );
                if (matcher.isUserMatching(user)) {
                    switch (rule.getRuleDirection()) {
                        case INCOMING ->
                            matchingRules.incoming.add(rule.getId());
                        case OUTGOING ->
                            matchingRules.outgoing.add(rule.getId());
                    }
                }
            }
        }
        return matchingRules;
    }

    @GetMapping("/user_rules")
    @RolesAllowed(value = {"USER"})
    public List<RichRule> getUserRules(
            @RequestParam(required = false, name = "type") String type
    ) throws HttpResponseException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserModel user = userRepository.findByUsername(username);
        if (user == null) {
            user = ldapUserSource.findUser(username);
        }
        List< RichRule> richRules = new LinkedList<>();

        if (user == null) {
            String msg = "User %s doesn't exist -> no firewall rules"
                    .formatted(username);
            log.warn(msg);
            throw new HttpResponseException(
                    HttpStatus.NOT_FOUND.value(),
                    msg
            );
        }

        try {
            firewallController.writeRules(FirewallRuleModel.VpnType.USER);
        } catch (IOException | JSONException ex) {
            log.error("Cannot write firewall rules: " + ex.getMessage());
            throw new HttpResponseException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    username
            );
        }

        log.debug("Get firewall rules for " + username);
        for (FirewallRuleModel rule : firewallRuleRepository.findAllByVpnTypeAndRuleDirection(
                FirewallRuleModel.VpnType.USER,
                FirewallRuleModel.RuleDirection.INCOMING
        )) {
            if (!rule.isEnabled()) {
                log.debug("Ignoring disabled rule" + rule.toString());
                continue;
            }
            boolean matches = false;
            for (FirewallWho who : rule.getWho()) {
                if (who.getUserMatcherClassName().equals(EverybodyMatcher.class.getName())) {
                    log.debug("Ignoring everybody rule" + rule.toString());
                    break;
                }
                UserMatcher matcher = userMatcherCollector.buildUserMatcher(
                        who.getUserMatcherClassName(),
                        who.getParameter()
                );
                if (matcher.isUserMatching(user)) {
                    matches = true;
                    log.debug(
                            "Rule %s matches user %s"
                                    .formatted(rule.toString(), username)
                    );
                    break;
                }
                log.debug(
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
    @RolesAllowed(value = {"USER"})
    public UserFirewallEverybodyRules getEverybodyRules() {
        UserFirewallEverybodyRules firewallEveryBodyRules = new UserFirewallEverybodyRules();
        List<RichRule> richRules = new LinkedList<>();

        for (FirewallRuleModel rule : firewallRuleRepository.findAllByVpnTypeAndRuleDirection(
                FirewallRuleModel.VpnType.USER,
                FirewallRuleModel.RuleDirection.INCOMING
        )) {
            if (rule.isEnabled()) {
                for (FirewallWho who : rule.getWho()) {
                    if (who.getUserMatcherClassName().equals(EverybodyMatcher.class.getName())) {
                        richRules.addAll(createRichRules(rule));
                    }
                }
            }
        }
        firewallEveryBodyRules.setRichRules(richRules);
        UserFirewallBasicsSettings firewallBasicsSettings = settings.getSettings(UserFirewallBasicsSettings.class);
        firewallEveryBodyRules.setIcmpRules(firewallBasicsSettings.getIcmpRules());

        return firewallEveryBodyRules;
    }

    private List<RichRule> createRichRules(FirewallRuleModel rule) {
        List<RichRule> richRules = new LinkedList<>();
        OpenVpnUserSettings openvpnSettings = settings.getSettings(OpenVpnUserSettings.class);

        Set<String> addresses = new HashSet<>();
        for (FirewallWhere where : rule.getTo()) {
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
