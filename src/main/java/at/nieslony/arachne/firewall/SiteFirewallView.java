/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.firewall.basicsettings.SiteFirewallBasicsSettings;
import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.firewall.basicsettings.EnableRoutingMode;
import at.nieslony.arachne.firewall.basicsettings.IcmpRules;
import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.openvpn.OpenVpnController;
import at.nieslony.arachne.openvpn.OpenVpnSiteSettings;
import at.nieslony.arachne.openvpnmanagement.ArachneDbus;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.utils.components.ShowNotification;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.LinkedList;
import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 *
 * @author claas
 */
@Route(value = "siteVpn/firewall", layout = ViewTemplate.class)
@PageTitle("Site 2 Site VPN | Firewall")
@RolesAllowed("ADMIN")
@Slf4j
public class SiteFirewallView extends VerticalLayout {

    private final FirewallRulesEditor incomingRulesEditor;

    private final Settings settings;
    private final OpenVpnController openVpnRestController;
    private final ArachneDbus arachneDbus;

    private SiteFirewallBasicsSettings firewallBasicsSettings;
    private final LdapSettings ldapSettings;
    private final Binder<SiteFirewallBasicsSettings> binder;

    public SiteFirewallView(
            FirewallRuleRepository firewallRuleRepository,
            UserMatcherCollector userMatcherCollector,
            Settings settings,
            OpenVpnController openVpnController,
            ArachneDbus arachneDbus
    ) {
        this.settings = settings;
        this.openVpnRestController = openVpnController;
        this.arachneDbus = arachneDbus;

        binder = new Binder<>();
        firewallBasicsSettings = settings.getSettings(SiteFirewallBasicsSettings.class);
        ldapSettings = settings.getSettings(LdapSettings.class);

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();

        tabs.add(
                "Basics",
                createBasicsTab()
        );

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

    private Component createBasicsTab() {
        VerticalLayout layout = new VerticalLayout();

        Checkbox enableFirewallField = new Checkbox("Enable Firewall");
        enableFirewallField.setValue(true);
        binder.forField(enableFirewallField)
                .bind(
                        SiteFirewallBasicsSettings::isEnableFirewall,
                        SiteFirewallBasicsSettings::setEnableFirewall
                );

        TextField firewallZoneField = new TextField("Firewall Zone");
        firewallZoneField.setMaxLength(21 - 4); // max len 21 - len("-out") for policy
        binder.forField(firewallZoneField)
                .bind(
                        SiteFirewallBasicsSettings::getFirewallZone,
                        SiteFirewallBasicsSettings::setFirewallZone
                );

        RadioButtonGroup<EnableRoutingMode> enableRoutingMode
                = new RadioButtonGroup<>("Enable Routing");
        enableRoutingMode.setItems(EnableRoutingMode.values());
        binder.forField(enableRoutingMode)
                .bind(
                        SiteFirewallBasicsSettings::getEnableRoutingMode,
                        SiteFirewallBasicsSettings::setEnableRoutingMode
                );

        Select<IcmpRules> icmpRules = new Select<>();
        icmpRules.setLabel("Allow PING");
        icmpRules.setItems(IcmpRules.values());
        icmpRules.setMinWidth("20em");
        binder.bind(
                icmpRules,
                SiteFirewallBasicsSettings::getIcmpRules,
                SiteFirewallBasicsSettings::setIcmpRules
        );

        Button saveButton = new Button("Save and Restart VPN", (e) -> {
            OpenVpnSiteSettings openVpnSiteSettings = settings.getSettings(OpenVpnSiteSettings.class);

            log.debug("Saving firewall settings: " + firewallBasicsSettings.toString());
            try {
                firewallBasicsSettings.save(settings);
                openVpnRestController.writeOpenVpnPluginSiteConfig(
                        openVpnSiteSettings,
                        firewallBasicsSettings
                );
                arachneDbus.restartServer(ArachneDbus.ServerType.SITE);
                ShowNotification.info("OpenVpn restarted with new configuration");

            } catch (SettingsException ex) {
                log.error("Cannot save firewall settings: " + ex.getMessage());
            } catch (DBusException | DBusExecutionException ex) {
                log.error("Cannot restart openVPN: " + ex.getMessage());
                ShowNotification.error("Cannot restart openVPN", ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        enableFirewallField.addValueChangeListener((e) -> {
            boolean isEnabled = e.getValue();
            firewallZoneField.setEnabled(isEnabled);
            enableRoutingMode.setEnabled(isEnabled);
            icmpRules.setEnabled(isEnabled);
        });

        layout.add(
                enableFirewallField,
                firewallZoneField,
                enableRoutingMode,
                icmpRules,
                saveButton
        );
        layout.setMargin(false);
        layout.setPadding(false);

        log.debug("Loading firewall settings: " + firewallBasicsSettings.toString());
        binder.setBean(firewallBasicsSettings);

        return layout;
    }
}
