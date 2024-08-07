/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.ViewTemplate;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 *
 * @author claas
 */
@Route(value = "siteVpn/firewall", layout = ViewTemplate.class)
@PageTitle("Site 2 Site VPN | Firewall")
@RolesAllowed("ADMIN")
public class SiteFirefallView extends VerticalLayout {

    private FirewallRulesEditor incomingRulesEditor;

    public SiteFirefallView(FirewallRuleRepository fireRuleRepository) {
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();

        incomingRulesEditor = new FirewallRulesEditor(
                fireRuleRepository,
                FirewallRuleModel.VpnType.SITE,
                FirewallRuleModel.RuleDirection.INCOMING
        );
        tabs.add(
                "Incoming",
                incomingRulesEditor
        );

        add(tabs);
        setPadding(false);
    }
}
