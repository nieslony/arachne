/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.openvpnmanagement.ArachneDbus;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import java.util.LinkedList;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 *
 * @author claas
 */
@Route(value = "siteVpn/firewall", layout = ViewTemplate.class)
@PageTitle("Site 2 Site VPN | Firewall")
@RolesAllowed("ADMIN")
public class SiteFirewallView extends AbstractFirewallView<SiteFirewallBasicsSettings> {

    @PostConstruct
    public void init() {
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();

        LdapSettings ldapSettings = settings.getSettings(LdapSettings.class);

        FirewallRulesEditor incomingRulesEditor = new FirewallRulesEditor(
                firewallRuleRepository,
                userMatcherCollector,
                ldapSettings,
                firewallController,
                FirewallRuleModel.VpnType.SITE,
                FirewallRuleModel.RuleDirection.INCOMING
        );
        FirewallRulesEditor outgoingRulesEditor = new FirewallRulesEditor(
                firewallRuleRepository,
                userMatcherCollector,
                ldapSettings,
                firewallController,
                FirewallRuleModel.VpnType.SITE,
                FirewallRuleModel.RuleDirection.OUTGOING
        );
        tabs.add("Basics", createBasicsTab(SiteFirewallBasicsSettings.class));
        tabs.add("Incoming Rules", incomingRulesEditor);
        tabs.add("Outgoing Rules", outgoingRulesEditor);
        tabs.setHeightFull();

        if (firewallRuleRepository.countByVpnTypeAndRuleDirection(
                FirewallRuleModel.VpnType.SITE,
                FirewallRuleModel.RuleDirection.INCOMING
        ) == 0) {
            createDefaultSiteRule();
        }

        add(tabs);
        setPadding(false);
    }

    private void createDefaultSiteRule() {
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

    @Override
    protected void applyBasicSettings(SiteFirewallBasicsSettings basicSettings) throws DBusException {
        openVpnController.writeOpenVpnPluginSiteConfig(
                null,
                firewallBasicSettings);
        arachneDbus.restartServer(ArachneDbus.ServerType.SITE);
    }
}
