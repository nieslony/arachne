/*
 * Copyright (C) 2025 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.firewall.settings.AbstractFirewallBasicsSettings;
import at.nieslony.arachne.firewall.settings.EnableRoutingMode;
import at.nieslony.arachne.firewall.settings.IcmpRules;
import at.nieslony.arachne.openvpn.OpenVpnController;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author claas
 */
@Slf4j
abstract public class AbstractFirewallView<BasicSettings extends AbstractFirewallBasicsSettings> extends VerticalLayout {

    protected BasicSettings firewallBasicSettings;

    @Autowired
    protected OpenVpnController openVpnController;

    @Autowired
    protected ArachneDbus arachneDbus;

    @Autowired
    protected Settings settings;

    @Autowired
    protected FirewallRuleRepository firewallRuleRepository;

    @Autowired
    protected UserMatcherCollector userMatcherCollector;

    @Autowired
    protected FirewallController firewallController;

    protected Component createBasicsTab(Class<BasicSettings> basicSettingsClass) {
        firewallBasicSettings = settings.getSettings(basicSettingsClass);

        VerticalLayout layout = new VerticalLayout();
        Binder<AbstractFirewallBasicsSettings> binder = new Binder<>();

        Checkbox enableFirewallField = new Checkbox("Enable Firewall");
        enableFirewallField.setValue(true);
        binder.forField(enableFirewallField)
                .bind(
                        AbstractFirewallBasicsSettings::isEnableFirewall,
                        AbstractFirewallBasicsSettings::setEnableFirewall
                );

        TextField firewallZoneField = new TextField("Firewall Zone");
        firewallZoneField.setMaxLength(21 - 4); // max len 21 - len("-out") for policy
        binder.forField(firewallZoneField)
                .bind(
                        AbstractFirewallBasicsSettings::getFirewallZone,
                        AbstractFirewallBasicsSettings::setFirewallZone
                );

        RadioButtonGroup<EnableRoutingMode> enableRoutingMode
                = new RadioButtonGroup<>("Enable Routing");
        enableRoutingMode.setItems(EnableRoutingMode.values());
        binder.forField(enableRoutingMode)
                .bind(
                        AbstractFirewallBasicsSettings::getEnableRoutingMode,
                        AbstractFirewallBasicsSettings::setEnableRoutingMode
                );

        Select<IcmpRules> icmpRules = new Select<>();
        icmpRules.setLabel("Allow PING");
        icmpRules.setItems(IcmpRules.values());
        icmpRules.setMinWidth("20em");
        binder.bind(
                icmpRules,
                AbstractFirewallBasicsSettings::getIcmpRules,
                AbstractFirewallBasicsSettings::setIcmpRules
        );

        Button saveButton = new Button("Save and Restart VPN", (e) -> {
            log.debug("Saving firewall settings: " + firewallBasicSettings.toString());
            try {
                firewallBasicSettings.save(settings);
                applyBasicSettings(firewallBasicSettings);
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

        binder.setBean(firewallBasicSettings);

        return layout;
    }

    abstract protected void applyBasicSettings(BasicSettings basicSettings) throws DBusException;
}
