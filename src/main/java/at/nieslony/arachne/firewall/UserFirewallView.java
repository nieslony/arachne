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

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.openvpnmanagement.ArachneDbus;
import at.nieslony.arachne.usermatcher.EverybodyMatcher;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import java.util.LinkedList;
import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 *
 * @author claas
 */
@Route(value = "userVpn/firewall", layout = ViewTemplate.class)
@PageTitle("User VPN | Firewall")
@RolesAllowed("ADMIN")
@Slf4j
public class UserFirewallView extends AbstractFirewallView<UserFirewallBasicsSettings> {

    private LdapSettings ldapSettings;

    @PostConstruct
    public void init() {
        ldapSettings = settings.getSettings(LdapSettings.class);

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("Basics", createBasicsTab(UserFirewallBasicsSettings.class));
        tabs.add("Incoming Rules", new FirewallRulesEditor(
                firewallRuleRepository,
                userMatcherCollector,
                ldapSettings,
                firewallController,
                FirewallRuleModel.VpnType.USER,
                FirewallRuleModel.RuleDirection.INCOMING
        ));
        tabs.add("Outgoing Rules", new FirewallRulesEditor(
                firewallRuleRepository,
                userMatcherCollector,
                ldapSettings,
                firewallController,
                FirewallRuleModel.VpnType.USER,
                FirewallRuleModel.RuleDirection.OUTGOING
        ));

        if (firewallRuleRepository.countByVpnTypeAndRuleDirection(
                FirewallRuleModel.VpnType.USER,
                FirewallRuleModel.RuleDirection.INCOMING
        ) == 0) {
            FirewallRuleModel rule = new FirewallRuleModel(
                    FirewallRuleModel.VpnType.USER,
                    FirewallRuleModel.RuleDirection.INCOMING
            );
            rule.setDescription("Allow DNS access for everybody");

            FirewallWho who = new FirewallWho();
            who.setUserMatcherClassName(EverybodyMatcher.class.getName());
            rule.setWho(new LinkedList<>());
            rule.getWho().add(who);

            FirewallWhere to = new FirewallWhere();
            to.setType(FirewallWhere.Type.PushedDnsServers);
            rule.setTo(new LinkedList<>());
            rule.getTo().add(to);

            FirewallWhat what = new FirewallWhat();
            what.setType(FirewallWhat.Type.Service);
            what.setService("dns");
            rule.setWhat(new LinkedList<>());
            rule.getWhat().add(what);

            rule.setEnabled(true);

            firewallRuleRepository.save(rule);
        }

        tabs.setHeightFull();

        add(tabs);
        setPadding(false);
    }

    @Override
    protected void applyBasicSettings(UserFirewallBasicsSettings basicSettings) throws DBusException {
        OpenVpnUserSettings openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);
        openVpnController.writeOpenVpnPluginUserConfig(
                openVpnUserSettings,
                firewallBasicSettings
        );
        arachneDbus.restartServer(ArachneDbus.ServerType.USER);
    }
}
