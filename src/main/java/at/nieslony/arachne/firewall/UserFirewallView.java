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
import at.nieslony.arachne.openvpn.OpenVpnRestController;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "userVpn/firewall", layout = ViewTemplate.class)
@PageTitle("User VPN | Firewall")
@RolesAllowed("ADMIN")
public class UserFirewallView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(UserFirewallView.class);

    private final FirewallRuleRepository firewallRuleRepository;
    private final UserMatcherCollector userMatcherCollector;
    private final OpenVpnRestController openVpnRestController;
    private final Settings settings;

    private final Binder<UserFirewallBasicsSettings> binder;
    private final UserFirewallBasicsSettings firewallBasicSettings;
    private final LdapSettings ldapSettings;

    public UserFirewallView(
            FirewallRuleRepository firewallRuleRepository,
            UserMatcherCollector userMatcherCollector,
            OpenVpnRestController openVpnRestController,
            Settings settings
    ) {
        this.firewallRuleRepository = firewallRuleRepository;
        this.userMatcherCollector = userMatcherCollector;
        this.openVpnRestController = openVpnRestController;
        this.settings = settings;

        binder = new Binder<>();
        firewallBasicSettings = settings.getSettings(UserFirewallBasicsSettings.class);
        ldapSettings = settings.getSettings(LdapSettings.class);

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("Basics", createBasicsTab());
        tabs.add("Incoming Rules", new FirewallRulesEditor(
                firewallRuleRepository,
                userMatcherCollector,
                ldapSettings,
                FirewallRuleModel.VpnType.USER,
                FirewallRuleModel.RuleDirection.INCOMING
        ));

        add(tabs);
        setPadding(false);
    }

    private Component createBasicsTab() {
        VerticalLayout layout = new VerticalLayout();

        Checkbox enableFirewallField = new Checkbox("Enable Firewall");
        enableFirewallField.setValue(true);
        binder.forField(enableFirewallField)
                .bind(UserFirewallBasicsSettings::isEnableFirewall, UserFirewallBasicsSettings::setEnableFirewall);

        TextField firewallZoneField = new TextField("Firewall Zone");
        binder.forField(firewallZoneField)
                .bind(UserFirewallBasicsSettings::getFirewallZone, UserFirewallBasicsSettings::setFirewallZone);

        RadioButtonGroup<UserFirewallBasicsSettings.EnableRoutingMode> enableRoutingMode
                = new RadioButtonGroup<>("Enable Routing");
        enableRoutingMode.setItems(UserFirewallBasicsSettings.EnableRoutingMode.values());
        binder.forField(enableRoutingMode)
                .bind(UserFirewallBasicsSettings::getEnableRoutingMode, UserFirewallBasicsSettings::setEnableRoutingMode);

        Select<UserFirewallBasicsSettings.IcmpRules> icmpRules = new Select<>();
        icmpRules.setLabel("Allow PING");
        icmpRules.setItems(UserFirewallBasicsSettings.IcmpRules.values());
        icmpRules.setMinWidth("20em");
        binder.bind(
                icmpRules,
                UserFirewallBasicsSettings::getIcmpRules,
                UserFirewallBasicsSettings::setIcmpRules
        );

        Button saveButton = new Button("Save", (e) -> {
            OpenVpnUserSettings openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);

            logger.info("Saving firewall settings");
            try {
                firewallBasicSettings.save(settings);
                openVpnRestController.writeOpenVpnPluginUserConfig(
                        openVpnUserSettings,
                        firewallBasicSettings
                );
            } catch (SettingsException ex) {
                logger.error("Cannot save firewall settings: " + ex.getMessage());
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

        binder.setBean(firewallBasicSettings);

        return layout;
    }
}
