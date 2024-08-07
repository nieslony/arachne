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
import at.nieslony.arachne.usermatcher.EverybodyMatcher;
import at.nieslony.arachne.usermatcher.LdapGroupUserMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.usermatcher.UserMatcherInfo;
import at.nieslony.arachne.usermatcher.UsernameMatcher;
import at.nieslony.arachne.utils.UsersGroupsAutocomplete;
import at.nieslony.arachne.utils.net.NetMask;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
import at.nieslony.arachne.utils.validators.HostnameValidator;
import at.nieslony.arachne.utils.validators.IgnoringInvisibleOrDisabledValidator;
import at.nieslony.arachne.utils.validators.RequiredIfVisibleValidator;
import at.nieslony.arachne.utils.validators.SubnetValidator;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "userVpn/firewall", layout = ViewTemplate.class)
@PageTitle("Firewall")
@RolesAllowed("ADMIN")
public class UserFirewallView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(UserFirewallView.class);

    final private FirewallRuleRepository firewallRuleRepository;
    final private UserMatcherCollector userMatcherCollector;

    private final Binder<UserFirewallBasicsSettings> binder;
    private UserFirewallBasicsSettings firewallBasicSettings;
    private final LdapSettings ldapSettings;

    public UserFirewallView(
            FirewallRuleRepository firewallRuleRepository,
            UserMatcherCollector userMatcherCollector,
            OpenVpnRestController openVpnRestController,
            Settings settings
    ) {
        this.firewallRuleRepository = firewallRuleRepository;
        this.userMatcherCollector = userMatcherCollector;

        binder = new Binder<>();
        firewallBasicSettings = settings.getSettings(UserFirewallBasicsSettings.class);
        ldapSettings = settings.getSettings(LdapSettings.class);

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("Basics", createBasicsTab());
        tabs.add("Incoming Rules", createIncomingTab());

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

        add(tabs, saveButton);
        setPadding(false);
    }

    private Component createBasicsTab() {
        VerticalLayout layout = new VerticalLayout();

        Checkbox enableFirewallField = new Checkbox("Enable Firewall");
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

        binder.setBean(firewallBasicSettings);

        layout.add(
                enableFirewallField,
                firewallZoneField,
                enableRoutingMode
        );
        return layout;
    }

    private Component createIncomingTab() {
        Select<UserFirewallBasicsSettings.IcmpRules> icmpRules = new Select<>();
        icmpRules.setItems(UserFirewallBasicsSettings.IcmpRules.values());
        icmpRules.setMinWidth("20em");
        binder.bind(
                icmpRules,
                UserFirewallBasicsSettings::getIcmpRules,
                UserFirewallBasicsSettings::setIcmpRules
        );
        icmpRules.setLabel("Allow PING");

        Grid<FirewallRuleModel> grid = new Grid<>();
        grid.setWidthFull();

        grid
                .addColumn(new ComponentRenderer<>(
                        (var model) -> {
                            Collection<FirewallWho> whos = model.getWho();
                            return switch (whos.size()) {
                        case 0 ->
                            new Text("");
                        case 1 ->
                            new Text(whos.toArray()[0].toString());
                        default ->
                            createDetails(whos);
                    };
                        }
                ))
                .setHeader("Who")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid
                .addColumn(new ComponentRenderer<>(
                        (var model) -> {
                            Collection<FirewallWhere> wheres = model.getTo();
                            return switch (wheres.size()) {
                        case 0 ->
                            new Text("");
                        case 1 ->
                            new Text(wheres.toArray()[0].toString());
                        default ->
                            createDetails(wheres);
                    };
                        }
                ))
                .setHeader("Where")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid
                .addColumn(new ComponentRenderer<>(
                        (var model) -> {
                            Collection<FirewallWhat> whats = model.getWhat();
                            return switch (whats.size()) {
                        case 0 ->
                            new Text("");
                        case 1 ->
                            new Text(whats.toArray()[0].toString());
                        default ->
                            createDetails(whats);
                    };
                        }
                ))
                .setHeader("What")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(FirewallRuleModel::isEnabled)
                .setHeader("Enabled")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid
                .addColumn(FirewallRuleModel::getDescription)
                .setHeader("Description")
                .setFlexGrow(1);

        grid
                .addColumn(new ComponentRenderer<>(
                        (model) -> {
                            MenuBar menuBar = new MenuBar();
                            menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
                            MenuItem menuItem = menuBar.addItem(new Icon(VaadinIcon.CHEVRON_DOWN));
                            SubMenu submenu = menuItem.getSubMenu();
                            submenu.addItem(
                                    "Edit...",
                                    event -> editRule(grid, model)
                            );
                            submenu.addItem(
                                    "Delete...",
                                    event -> deleteRule(grid, model)
                            );

                            return menuBar;
                        }
                ))
                .setWidth("5em")
                .setFlexGrow(0);

        Button addRule = new Button("Add...", e -> {
            FirewallRuleModel rule = new FirewallRuleModel(
                    FirewallRuleModel.VpnType.USER,
                    FirewallRuleModel.RuleDirection.INCOMING
            );
            editRule(grid, rule);
        });

        if (firewallRuleRepository.count() == 0) {
            FirewallRuleModel allowDns = new FirewallRuleModel(
                    FirewallRuleModel.VpnType.USER,
                    FirewallRuleModel.RuleDirection.INCOMING
            );
            allowDns.setDescription("Allow DNS acces for everybody");
            allowDns.setEnabled(true);

            FirewallWho who = new FirewallWho();
            who.setUserMatcherClassName(EverybodyMatcher.class.getName());
            allowDns.setWho(new LinkedList<>());
            allowDns.getWho().add(who);

            FirewallWhere where = new FirewallWhere();
            where.setType(FirewallWhere.Type.PushedDnsServers);
            allowDns.setTo(new LinkedList<>());
            allowDns.getTo().add(where);

            FirewallWhat what = new FirewallWhat();
            what.setType(FirewallWhat.Type.Service);
            what.setService("dns");
            allowDns.setWhat(new LinkedList<>());
            allowDns.getWhat().add(what);
            logger.info("What: " + allowDns.getWhat().toString());
            logger.info("Rule: " + allowDns.toString());

            firewallRuleRepository.save(allowDns);
        }

        grid.setItems(firewallRuleRepository.findAllByVpnTypeAndRuleDirection(
                FirewallRuleModel.VpnType.USER,
                FirewallRuleModel.RuleDirection.INCOMING
        ));

        VerticalLayout layout = new VerticalLayout(
                icmpRules,
                addRule,
                grid
        );
        layout.setWidthFull();

        return layout;
    }

    private void deleteRule(Grid<FirewallRuleModel> grid, FirewallRuleModel rule) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete firewall rule");
        confirm.setText(
                "Are you sure you want to permanently firewall rule?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(
                e -> {
                    firewallRuleRepository.delete(rule);
                    grid.setItems(firewallRuleRepository.findAllByVpnTypeAndRuleDirection(
                            FirewallRuleModel.VpnType.USER,
                            FirewallRuleModel.RuleDirection.INCOMING
                    ));
                });

        confirm.open();

    }

    private void editRule(Grid<FirewallRuleModel> grid, FirewallRuleModel rule) {
        logger.info(rule.toString());
        Dialog dlg = new Dialog();
        if (rule.getId() == null) {
            dlg.setHeaderTitle("New rule");
        } else {
            dlg.setHeaderTitle("Edit rule");
        }

        Binder<FirewallRuleModel> editRuleBinder = new Binder<>();

        NativeLabel whoLabel = new NativeLabel("Who");
        whoLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);
        ListBox<FirewallWho> whoList = new ListBox<>();
        whoList.setHeight(30, Unit.EX);
        whoList.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        whoList.setWidthFull();
        if (rule.getWho() != null) {
            whoList.setItems(rule.getWho());
        }
        Button addWhoButton = new Button("Add...");
        Button editWhoButton = new Button("Edit...");
        editWhoButton.setEnabled(false);
        Button removeWhoButton = new Button("Remove");
        removeWhoButton.setEnabled(false);
        VerticalLayout editWho = new VerticalLayout(
                whoLabel,
                whoList,
                new HorizontalLayout(
                        addWhoButton,
                        editWhoButton,
                        removeWhoButton
                )
        );
        editWho.setPadding(false);

        NativeLabel whereLabel = new NativeLabel("Where");
        whereLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);
        ListBox<FirewallWhere> whereList = new ListBox<>();
        whereList.setHeight(30, Unit.EX);
        whereList.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        if (rule.getTo() != null) {
            whereList.setItems(rule.getTo());
        }
        whereList.setWidthFull();
        Button addWhereButton = new Button("Add...");
        Button editWhereButton = new Button("Edit...");
        editWhereButton.setEnabled(false);
        Button removeWhereButton = new Button("Remove");
        removeWhereButton.setEnabled(false);

        VerticalLayout editWhere = new VerticalLayout(
                whereLabel,
                whereList,
                new HorizontalLayout(
                        addWhereButton,
                        editWhereButton,
                        removeWhereButton
                )
        );
        editWhere.setPadding(false);

        NativeLabel whatLabel = new NativeLabel("What");
        whatLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);
        ListBox<FirewallWhat> whatList = new ListBox<>();
        whatList.setHeight(30, Unit.EX);
        whatList.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        if (rule.getWhat() != null) {
            whatList.setItems(rule.getWhat());
        }
        whatList.setWidthFull();
        Button addWhatButton = new Button("Add...");
        Button editWhatButton = new Button("Edit...");
        editWhatButton.setEnabled(false);
        Button removeWhatButton = new Button("Remove");
        removeWhatButton.setEnabled(false);
        VerticalLayout editWhat = new VerticalLayout(
                whatLabel,
                whatList,
                new HorizontalLayout(
                        addWhatButton,
                        editWhatButton,
                        removeWhatButton
                )
        );
        editWhat.setPadding(false);

        TextField descriptionField = new TextField("Description");
        descriptionField.setWidthFull();
        descriptionField.setClearButtonVisible(true);
        editRuleBinder.forField(descriptionField)
                .bind(FirewallRuleModel::getDescription, FirewallRuleModel::setDescription);

        Checkbox isEnabledField = new Checkbox("Enable Rule");
        editRuleBinder.forField(isEnabledField)
                .bind(FirewallRuleModel::isEnabled, FirewallRuleModel::setEnabled);

        VerticalLayout layout = new VerticalLayout(
                new HorizontalLayout(
                        editWho,
                        editWhere,
                        editWhat
                ),
                descriptionField,
                isEnabledField
        );
        layout.setPadding(false);
        dlg.add(layout);

        Button saveButton = new Button("Save", e -> {
            firewallRuleRepository.save(rule);
            dlg.close();
            grid.setItems(firewallRuleRepository.findAllByVpnTypeAndRuleDirection(
                    FirewallRuleModel.VpnType.USER,
                    FirewallRuleModel.RuleDirection.INCOMING
            ));
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setAutofocus(true);
        saveButton.setEnabled(rule.isValid());
        Button cancelButton = new Button("Cancel", e -> dlg.close());

        whoList.addValueChangeListener((e) -> {
            editWhoButton.setEnabled(e.getValue() != null);
            removeWhoButton.setEnabled(e.getValue() != null);
        });

        whereList.addValueChangeListener((e) -> {
            editWhereButton.setEnabled(e.getValue() != null);
            removeWhereButton.setEnabled(e.getValue() != null);
        });

        whatList.addValueChangeListener((e) -> {
            editWhatButton.setEnabled(e.getValue() != null);
            removeWhatButton.setEnabled(e.getValue() != null);
        });

        addWhoButton.addClickListener((e) -> {
            FirewallWho who = new FirewallWho();
            who.setUserMatcherClassName(
                    EverybodyMatcher.class
                            .getName()
            );
            editWho(who, (FirewallWho w) -> {
                List<FirewallWho> l = rule.getWho();
                if (l == null) {
                    l = new LinkedList<>();
                    rule.setWho(l);
                }
                l.add(w);
                whoList.setItems(l);
                saveButton.setEnabled(rule.isValid());
            });
        });

        editWhoButton.addClickListener((e) -> {
            FirewallWho who = whoList.getValue();
            editWho(who, (FirewallWho w) -> {
                List<FirewallWho> l = rule.getWho();
                whoList.setItems(l);
                saveButton.setEnabled(rule.isValid());
            });
        });

        removeWhoButton.addClickListener((e) -> {
            FirewallWho who = whoList.getValue();
            List<FirewallWho> l = rule.getWho();
            l.remove(who);
            whoList.setItems(l);
            saveButton.setEnabled(rule.isValid());
        });

        addWhereButton.addClickListener((e) -> {
            FirewallWhere where = new FirewallWhere();
            editWhere(where, (FirewallWhere w) -> {
                List<FirewallWhere> l = rule.getTo();
                if (l == null) {
                    l = new LinkedList<>();
                    rule.setTo(l);
                }
                l.add(w);
                whereList.setItems(l);
                saveButton.setEnabled(rule.isValid());
            });
        });
        editWhereButton.addClickListener((e) -> {
            FirewallWhere where = whereList.getValue();
            editWhere(where, (FirewallWhere w) -> {
                List<FirewallWhere> l = rule.getTo();
                whereList.setItems(l);
            });
        });

        removeWhereButton.addClickListener((e) -> {
            FirewallWhere where = whereList.getValue();
            List<FirewallWhere> l = rule.getTo();
            l.remove(where);
            whereList.setItems(l);
            saveButton.setEnabled(rule.isValid());
        });

        addWhatButton.addClickListener((e) -> {
            FirewallWhat what = new FirewallWhat();
            editWhat(what, (w) -> {
                List<FirewallWhat> l = rule.getWhat();
                if (l == null) {
                    l = new LinkedList<>();
                    rule.setWhat(l);
                }
                l.add(w);
                whatList.setItems(l);
                saveButton.setEnabled(rule.isValid());
            });
        });

        editWhatButton.addClickListener((e) -> {
            FirewallWhat what = whatList.getValue();
            editWhat(what, (FirewallWhat w) -> {
                List<FirewallWhat> l = rule.getWhat();
                whatList.setItems(l);
                saveButton.setEnabled(rule.isValid());
            });
        });

        removeWhatButton.addClickListener((e) -> {
            FirewallWhat what = whatList.getValue();
            List<FirewallWhat> l = rule.getWhat();
            l.remove(what);
            whatList.setItems(l);
            saveButton.setEnabled(rule.isValid());
        });

        dlg.getFooter().add(cancelButton, saveButton);
        editRuleBinder.setBean(rule);

        dlg.open();
    }

    private void editWho(FirewallWho who, Consumer<FirewallWho> onSave) {
        logger.info("Editing who " + who.toString());
        Dialog dlg = new Dialog();
        if (who.getId() == null) {
            dlg.setHeaderTitle("Add Who");
        } else {
            dlg.setHeaderTitle("Edit Who");
        }

        Binder<FirewallWho> whoBinder = new Binder<>();

        Select<UserMatcherInfo> userMatchersSelect = new Select<>();
        userMatchersSelect.setLabel("User Matcher");
        userMatchersSelect.setItems(userMatcherCollector.getAllUserMatcherInfo());
        userMatchersSelect.setEmptySelectionAllowed(false);
        whoBinder.forField(userMatchersSelect)
                .bind(
                        rr -> {
                            return new UserMatcherInfo(rr.getUserMatcherClassName());
                        },
                        (rr, v) -> {
                            rr.setUserMatcherClassName(v.getClassName());
                        }
                );

        UsersGroupsAutocomplete parameterField
                = new UsersGroupsAutocomplete(ldapSettings, 5);
        whoBinder.forField(parameterField)
                .withValidator(
                        text -> {
                            String label = userMatchersSelect.getValue().getParameterLabel();
                            if (label == null || label.isEmpty()) {
                                return true;
                            }
                            return !parameterField.getValue().isEmpty();
                        },
                        "Value required")
                .bind(FirewallWho::getParameter, FirewallWho::setParameter);

        dlg.add(new VerticalLayout(
                userMatchersSelect,
                parameterField
        ));

        Button saveButton = new Button("Save", (t) -> {
            logger.info(who.toString());
            whoBinder.validate();
            if (whoBinder.isValid()) {
                dlg.close();
                onSave.accept(who);
            } else {
                t.getSource().setEnabled(false);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setAutofocus(true);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        userMatchersSelect.addValueChangeListener((e) -> {
            String labelTxt = e.getValue().getParameterLabel();
            parameterField.setLabel(labelTxt);
            if (labelTxt != null && !labelTxt.isEmpty()) {
                parameterField.setVisible(true);
                whoBinder.validate();
                saveButton.setEnabled(!parameterField.isInvalid());
            } else {
                parameterField.setVisible(false);
                saveButton.setEnabled(true);
            }
            String className = e.getValue().getClassName();
            if (className.equals(UsernameMatcher.class.getName())) {
                parameterField.setCompleteMode(
                        UsersGroupsAutocomplete.CompleteMode.USERS
                );
            } else if (className.equals(LdapGroupUserMatcher.class.getName())) {
                parameterField.setCompleteMode(
                        UsersGroupsAutocomplete.CompleteMode.GROUPS
                );
            } else {
                parameterField.setCompleteMode(
                        UsersGroupsAutocomplete.CompleteMode.NULL
                );
            }
        }
        );

        whoBinder.addStatusChangeListener((sce) -> {
            saveButton.setEnabled(!sce.hasValidationErrors());
        });

        whoBinder.setBean(who);
        whoBinder.validate();

        dlg.getFooter().add(cancelButton, saveButton);
        dlg.open();
    }

    private void editWhere(FirewallWhere where, Consumer<FirewallWhere> onSave) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Edit Where");

        Binder<FirewallWhere> whereBinder = new Binder<>();

        Select<FirewallWhere.Type> whereTypeSelect = new Select<>();
        whereTypeSelect.setLabel("Where Type");
        whereTypeSelect.setItems(FirewallWhere.Type.values());
        whereTypeSelect.setEmptySelectionAllowed(false);
        whereTypeSelect.setWidth(20, Unit.EM);
        whereBinder.forField(whereTypeSelect)
                .bind(FirewallWhere::getType, FirewallWhere::setType);

        TextField hostnameField = new TextField("Hostname");
        hostnameField.setWidthFull();
        hostnameField.setVisible(false);
        hostnameField.setValueChangeMode(ValueChangeMode.EAGER);
        whereBinder.forField(hostnameField)
                .asRequired(new IgnoringInvisibleOrDisabledValidator<>(
                        new HostnameValidator().withEmptyAllowed(false))
                )
                .bind(FirewallWhere::getHostname, FirewallWhere::setHostname);

        TextField networkField = new TextField("Network");
        networkField.setValueChangeMode(ValueChangeMode.EAGER);

        Select<NetMask> netMaskField = new Select<>();
        netMaskField.setItems(
                IntStream
                        .range(1, 32)
                        .boxed()
                        .map(i -> new NetMask(i))
                        .collect(Collectors.toList())
        );
        netMaskField.setLabel("Subnet Mask");
        netMaskField.setWidth(20, Unit.EM);
        whereBinder.forField(netMaskField)
                .bind(
                        (source) -> new NetMask(source.getSubnetMask()),
                        (dest, value) -> dest.setSubnetMask(value.getBits())
                );
        whereBinder.forField(networkField)
                .asRequired(new IgnoringInvisibleOrDisabledValidator<>(
                        new SubnetValidator(() -> {
                            NetMask mask = netMaskField.getValue();
                            if (mask == null) {
                                return 0;
                            } else {
                                return mask.getBits();
                            }
                        })
                ))
                .bind(FirewallWhere::getSubnet, FirewallWhere::setSubnet);

        HorizontalLayout networkEdit = new HorizontalLayout(
                networkField,
                new Text("/"),
                netMaskField
        );
        networkEdit.setFlexGrow(1, networkField);
        networkEdit.setWidthFull();
        networkEdit.setAlignItems(Alignment.BASELINE);
        networkEdit.setVisible(false);

        TextField serviceRecDomainField = new TextField("Domain");
        serviceRecDomainField.setPattern(("^[a-z][a-z9-9\\-]*(\\.[a-z][a-z0-9\\-]*)*$"));
        serviceRecDomainField.setValueChangeMode(ValueChangeMode.EAGER);
        whereBinder.forField(serviceRecDomainField)
                .asRequired(new RequiredIfVisibleValidator())
                .bind(FirewallWhere::getServiceRecDomain, FirewallWhere::setServiceRecDomain);

        TextField serviceRecNameField = new TextField("Service");
        serviceRecNameField.setWidth(10, Unit.EM);
        serviceRecNameField.setPattern("[a-z]*");
        serviceRecNameField.setValueChangeMode(ValueChangeMode.EAGER);
        whereBinder.forField(serviceRecNameField)
                .asRequired(new RequiredIfVisibleValidator())
                .bind(FirewallWhere::getServiceRecName, FirewallWhere::setServiceRecName);

        Select<TransportProtocol> serviceRecProtocolField = new Select<>();
        serviceRecProtocolField.setLabel("Protocol");
        serviceRecProtocolField.setItems(TransportProtocol.values());
        serviceRecProtocolField.setWidth(6, Unit.EM);
        serviceRecProtocolField.setEmptySelectionAllowed(false);
        whereBinder.forField(serviceRecProtocolField)
                .bind(FirewallWhere::getServiceRecProtocol, FirewallWhere::setServiceRecProtocol);

        HorizontalLayout serviceRecEdit = new HorizontalLayout(
                serviceRecDomainField,
                serviceRecNameField,
                serviceRecProtocolField
        );
        serviceRecEdit.setFlexGrow(2, serviceRecDomainField);
        serviceRecEdit.setFlexGrow(1, serviceRecNameField);
        serviceRecEdit.setVisible(false);
        serviceRecEdit.setWidthFull();

        TextField mxRecDomain = new TextField("Domain");
        mxRecDomain.setWidthFull();
        mxRecDomain.setValueChangeMode(ValueChangeMode.EAGER);
        whereBinder.forField(mxRecDomain)
                .asRequired(new IgnoringInvisibleOrDisabledValidator<>(
                        new HostnameValidator())
                )
                .bind(FirewallWhere::getMxDomain, FirewallWhere::setMxDomain);

        VerticalLayout layout = new VerticalLayout(
                whereTypeSelect,
                hostnameField,
                networkEdit,
                serviceRecEdit,
                mxRecDomain
        );
        layout.setPadding(false);
        layout.setSpacing(false);
        dlg.add(layout);

        Button saveButton = new Button("Save", (e) -> {
            dlg.close();
            onSave.accept(where);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setAutofocus(true);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        whereTypeSelect.addValueChangeListener((e) -> {
            hostnameField.setVisible(false);
            networkEdit.setVisible(false);
            serviceRecEdit.setVisible(false);
            mxRecDomain.setVisible(false);
            switch (e.getValue()) {
                case Hostname ->
                    hostnameField.setVisible(true);
                case Subnet -> {
                    networkEdit.setVisible(true);
                }
                case ServiceRecord -> {
                    serviceRecEdit.setVisible(true);
                    if (serviceRecDomainField.getValue() == null
                            || "".equals(serviceRecDomainField.getValue())) {
                        serviceRecDomainField.setValue(NetUtils.myDomain());
                    }
                    if (serviceRecProtocolField.getValue() == null) {
                        serviceRecProtocolField.setValue(TransportProtocol.TCP);
                    }
                }
                case MxRecord -> {
                    mxRecDomain.setVisible(true);

                    if (mxRecDomain.getValue() == null
                            || mxRecDomain.getValue().isEmpty()) {
                        mxRecDomain.setValue(NetUtils.myDomain());
                    }
                }
            }
            whereBinder.validate();
        });

        dlg.getFooter().add(cancelButton, saveButton);

        whereBinder.addStatusChangeListener((sce) -> {
            saveButton.setEnabled(!sce.hasValidationErrors());
        });

        whereBinder.setBean(where);
        whereBinder.validate();

        dlg.setMinWidth(40, Unit.EM);
        dlg.open();
    }

    private void editWhat(FirewallWhat what, Consumer<FirewallWhat> onSave) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Edit What");

        Binder<FirewallWhat> whatBinder = new Binder<>();
        Collection<FirewalldService> firewalldServices = FirewalldService.getAllServices();

        Select<FirewallWhat.Type> whatTypeSelect = new Select<>();
        whatTypeSelect.setLabel("What Type");
        whatTypeSelect.setItems(FirewallWhat.Type.values());
        whatBinder.forField(whatTypeSelect)
                .bind(FirewallWhat::getType, FirewallWhat::setType);

        ComboBox<FirewalldService> firewalldServiceSelect = new ComboBox<>();
        firewalldServiceSelect.setLabel("Firewalld Service");
        firewalldServiceSelect.setItems(firewalldServices);
        firewalldServiceSelect.setItemLabelGenerator(FirewalldService::getShortDescription);
        firewalldServiceSelect.setWidthFull();
        firewalldServiceSelect.setVisible(false);
        whatBinder.forField(firewalldServiceSelect)
                .bind(
                        (FirewallWhat source)
                        -> FirewalldService.getService(source.getService()),
                        (FirewallWhat dest, FirewalldService value)
                        -> dest.setService(value.getName())
                );

        IntegerField portField = new IntegerField("Port");
        portField.setMin(1);
        portField.setMax(65535);
        portField.setWidth(8, Unit.EM);
        portField.setStepButtonsVisible(true);
        whatBinder.forField(portField)
                .bind(FirewallWhat::getPort, FirewallWhat::setPort);

        Select<TransportProtocol> portProtocolSelect = new Select<>();
        portProtocolSelect.setItems(TransportProtocol.values());
        whatBinder.forField(portProtocolSelect)
                .bind(FirewallWhat::getPortProtocol, FirewallWhat::setPortProtocol);

        HorizontalLayout portEdit = new HorizontalLayout(
                portField,
                new Text("/"),
                portProtocolSelect
        );
        portEdit.setAlignItems(Alignment.BASELINE);
        portEdit.setVisible(false);
        portEdit.setWidthFull();

        IntegerField portFromField = new IntegerField("Port from");
        portFromField.setMin(1);
        portFromField.setMax(65535);
        portFromField.setWidth(8, Unit.EM);
        portFromField.setStepButtonsVisible(true);
        whatBinder.forField(portFromField)
                .bind(FirewallWhat::getPortFrom, FirewallWhat::setPortFrom);

        IntegerField portToField = new IntegerField("Port to");
        portToField.setMin(1);
        portToField.setMax(65535);
        portToField.setWidth(8, Unit.EM);
        portToField.setStepButtonsVisible(true);
        whatBinder.forField(portToField)
                .bind(FirewallWhat::getPortTo, FirewallWhat::setPortTo);

        Select<TransportProtocol> portRangeProtocolSelect = new Select<>();
        portRangeProtocolSelect.setItems(TransportProtocol.values());
        whatBinder.forField(portRangeProtocolSelect)
                .bind(FirewallWhat::getPortRangeProtocol, FirewallWhat::setPortRangeProtocol);

        HorizontalLayout portRangeEdit = new HorizontalLayout(
                portFromField,
                new Text("-"),
                portToField,
                new Text("/"),
                portRangeProtocolSelect
        );
        portRangeEdit.setAlignItems(Alignment.BASELINE);
        portRangeEdit.setVisible(false);
        portRangeEdit.setWidthFull();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.add(
                whatTypeSelect,
                firewalldServiceSelect,
                portEdit,
                portRangeEdit
        );
        dlg.add(layout);

        Button saveButton = new Button("Save", (e) -> {
            dlg.close();
            onSave.accept(what);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setAutofocus(true);

        Button cancelButtin = new Button("Cancel", (e) -> {
            dlg.close();
        });

        dlg.getFooter().add(cancelButtin, saveButton);

        whatTypeSelect.addValueChangeListener((e) -> {
            firewalldServiceSelect.setVisible(false);
            portEdit.setVisible(false);
            portRangeEdit.setVisible(false);

            switch (e.getValue()) {
                case Service ->
                    firewalldServiceSelect.setVisible(true);
                case OnePort ->
                    portEdit.setVisible(true);
                case PortRange ->
                    portRangeEdit.setVisible(true);
            }
        });

        whatBinder.setBean(what);
        whatBinder.validate();

        dlg.open();
    }

    private <T> Details createDetails(Collection<T> items) {
        UnorderedList detailItems = new UnorderedList();
        items.forEach((w) -> {
            detailItems.add(new ListItem(w.toString()));
        });
        detailItems.addClassName(LumoUtility.Padding.NONE);

        String summaryText = "%s...(%d)".formatted(
                items.toArray()[0].toString(),
                items.size()
        );

        Details details = new Details(summaryText, detailItems);
        details.addOpenedChangeListener((t) -> {
            if (t.isOpened()) {
                details.setSummaryText("%d rules".formatted(items.size()));
            } else {
                details.setSummaryText(summaryText);
            }
        });

        return details;
    }
}
