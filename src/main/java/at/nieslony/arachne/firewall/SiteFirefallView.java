/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.ViewTemplate;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
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

    private FirewallRuleRepository fireRuleRepository;

    public SiteFirefallView(FirewallRuleRepository fireRuleRepository) {
        this.fireRuleRepository = fireRuleRepository;

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("Incoming", createIncomingTab());

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(tabs, saveButton);
        setPadding(false);
    }

    private Component createIncomingTab() {
        Grid<FirewallRuleModel> grid = new Grid<>();
        grid.setWidthFull();

        Button addRule = new Button("Add...", e -> {
            FirewallRuleModel rule = new FirewallRuleModel(
                    FirewallRuleModel.VpnType.SITE,
                    FirewallRuleModel.RuleDirection.INCOMING
            );
            //editRule(grid, rule);
        });

        VerticalLayout layout = new VerticalLayout(
                addRule,
                grid
        );
        layout.setWidthFull();

        return layout;
    }
}
