/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.LinkedList;

/**
 *
 * @author claas
 */
@Route(value = "siteVpn/firewall", layout = ViewTemplate.class)
@PageTitle("Site 2 Site VPN | Firewall")
@RolesAllowed("ADMIN")
public class SiteFirefallView extends VerticalLayout {

    private FirewallRulesEditor incomingRulesEditor;

    public SiteFirefallView(
            FirewallRuleRepository firewallRuleRepository,
            UserMatcherCollector userMatcherCollector,
            Settings settings
    ) {
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();

        LdapSettings ldapSettings = settings.getSettings(LdapSettings.class);

        incomingRulesEditor = new FirewallRulesEditor(
                firewallRuleRepository,
                userMatcherCollector,
                ldapSettings,
                FirewallRuleModel.VpnType.SITE,
                FirewallRuleModel.RuleDirection.INCOMING
        );
        tabs.add(
                "Incoming",
                incomingRulesEditor
        );
        tabs.setHeightFull();

        if (firewallRuleRepository.countByVpnTypeAndRuleDirection(
                FirewallRuleModel.VpnType.SITE,
                FirewallRuleModel.RuleDirection.INCOMING
        ) == 0) {
            FirewallRuleModel rule = new FirewallRuleModel(
                    FirewallRuleModel.VpnType.SITE,
                    FirewallRuleModel.RuleDirection.INCOMING
            );
            rule.setDescription("Allow DNS access from all sites");

            FirewallWhere from = new FirewallWhere();
            from.setType(FirewallWhere.Type.Everywhere);
            rule.setFrom(new LinkedList<>());
            rule.getFrom().add(from);

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

        add(tabs);
        setPadding(false);
    }
}
