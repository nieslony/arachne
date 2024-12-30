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
import at.nieslony.arachne.openvpn.OpenVpnController;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.openvpnmanagement.ArachneDbus;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.usermatcher.EverybodyMatcher;
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
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
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
    private final OpenVpnController openVpnRestController;
    private final Settings settings;

    private final Binder<UserFirewallBasicsSettings> binder;
    private final UserFirewallBasicsSettings firewallBasicSettings;
    private final LdapSettings ldapSettings;
    private final ArachneDbus arachneDbus;

    public UserFirewallView(
            FirewallRuleRepository firewallRuleRepository,
            UserMatcherCollector userMatcherCollector,
            OpenVpnController openVpnRestController,
            ArachneDbus arachneDbus,
            Settings settings
    ) {
        this.firewallRuleRepository = firewallRuleRepository;
        this.userMatcherCollector = userMatcherCollector;
        this.openVpnRestController = openVpnRestController;
        this.arachneDbus = arachneDbus;
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

        Button saveButton = new Button("Save and Restart VPN", (e) -> {
            OpenVpnUserSettings openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);

            logger.info("Saving firewall settings");
            try {
                firewallBasicSettings.save(settings);
                openVpnRestController.writeOpenVpnPluginUserConfig(
                        openVpnUserSettings,
                        firewallBasicSettings
                );
                arachneDbus.restartServer(ArachneDbus.ServerType.USER);
                ShowNotification.info("OpenVpn restarted with new configuration");

            } catch (SettingsException ex) {
                logger.error("Cannot save firewall settings: " + ex.getMessage());
            } catch (DBusException | DBusExecutionException ex) {
                logger.error("Cannot restart openVPN: " + ex.getMessage());
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

        binder.setBean(firewallBasicSettings);

        return layout;
    }
}
