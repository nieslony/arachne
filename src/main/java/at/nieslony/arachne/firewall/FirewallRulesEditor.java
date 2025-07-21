/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.usermatcher.EverybodyMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.utils.components.MagicEditableListBox;
import at.nieslony.arachne.utils.components.ShowNotification;
import at.nieslony.arachne.utils.components.YesNoIcon;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
//import org.vaadin.firitin.layouts.HorizontalFloatLayout;

/**
 *
 * @author claas
 */
@Slf4j
class FirewallRulesEditor extends VerticalLayout {

    private final FirewallRuleRepository firewallRuleRepository;
    private final UserMatcherCollector userMatcherCollector;
    private final LdapSettings ldapSettings;

    private Grid<FirewallRuleModel> grid;

    public FirewallRulesEditor(
            FirewallRuleRepository firewallRuleRepository,
            UserMatcherCollector userMatcherCollector,
            LdapSettings ldapSettings,
            FirewallController firewallController,
            FirewallRuleModel.VpnType vpnType,
            FirewallRuleModel.RuleDirection direction
    ) {
        this.firewallRuleRepository = firewallRuleRepository;
        this.userMatcherCollector = userMatcherCollector;
        this.ldapSettings = ldapSettings;

        DataProvider<FirewallRuleModel, Void> dataProvider = DataProvider.fromCallbacks(
                query -> {
                    Pageable pageable = PageRequest.of(
                            query.getOffset(),
                            query.getLimit()
                    );
                    var page = firewallRuleRepository
                            .findAllByVpnTypeAndRuleDirection(
                                    vpnType,
                                    direction,
                                    pageable
                            );
                    return page.stream();
                },
                query -> (int) firewallRuleRepository
                        .countByVpnTypeAndRuleDirection(
                                vpnType,
                                direction
                        )
        );

        grid = new Grid<>();
        grid.setWidthFull();
        if (vpnType == FirewallRuleModel.VpnType.USER) {
            grid.addColumn(
                    new ComponentRenderer<>(
                            (var model) -> {
                                Collection<FirewallWho> who = model.getWho();
                                return switch (who.size()) {
                            case 0 ->
                                new Text("");
                            case 1 ->
                                new Text(who.toArray()[0].toString());
                            default ->
                                createDetails(who);
                        };
                            }))
                    .setHeader("Who")
                    .setAutoWidth(true)
                    .setFlexGrow(1);
        }
        if (vpnType == FirewallRuleModel.VpnType.SITE || direction == FirewallRuleModel.RuleDirection.OUTGOING) {
            grid.addColumn(
                    new ComponentRenderer<>(
                            (var model) -> {
                                Collection<FirewallWhere> from = model.getFrom();
                                return switch (from.size()) {
                            case 0 ->
                                new Text("");
                            case 1 ->
                                new Text(from.toArray()[0].toString());
                            default ->
                                createDetails(from);
                        };
                            }))
                    .setHeader("From")
                    .setAutoWidth(true)
                    .setFlexGrow(1);
        }
        if (vpnType == FirewallRuleModel.VpnType.SITE || direction == FirewallRuleModel.RuleDirection.INCOMING) {
            grid.addColumn(
                    new ComponentRenderer<>(
                            (var model) -> {
                                Collection<FirewallWhere> to = model.getTo();
                                return switch (to.size()) {
                            case 0 ->
                                new Text("");
                            case 1 ->
                                new Text(to.toArray()[0].toString());
                            default ->
                                createDetails(to);
                        };
                            }))
                    .setHeader("To")
                    .setAutoWidth(true)
                    .setFlexGrow(1);
        }
        grid.addColumn(
                new ComponentRenderer<>(
                        (var model) -> {
                            Collection<FirewallWhat> what = model.getWhat();
                            return switch (what.size()) {
                        case 0 ->
                            new Text("");
                        case 1 ->
                            new Text(what.toArray()[0].toString());
                        default ->
                            createDetails(what);
                    };
                        }))
                .setHeader("What")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(new ComponentRenderer<>(
                (var model) -> {
                    YesNoIcon icon = new YesNoIcon();
                    icon.setValue(model.isEnabled());
                    return icon;
                }))
                .setHeader("Enabled")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(FirewallRuleModel::getDescription)
                .setHeader("Description")
                .setFlexGrow(4);
        grid.addColumn(new ComponentRenderer<>(
                (var model) -> {
                    Button editButton = new Button(
                            VaadinIcon.EDIT.create(),
                            (e) -> editRule(model)
                    );
                    Button deleteButton = new Button(
                            VaadinIcon.DEL.create(),
                            (e) -> deleteRule(model)
                    );
                    HorizontalLayout layout = new HorizontalLayout(
                            editButton,
                            deleteButton
                    );
                    layout.setPadding(false);
                    layout.setMargin(false);
                    layout.setSpacing(false);
                    return layout;
                }))
                .setFlexGrow(0);
        grid.setEmptyStateText(switch (direction) {
            case INCOMING ->
                "All incoming traffic is blocked.";
            case OUTGOING ->
                "All outgoing traffic is blocked";
        });

        Button addRule = new Button("Add...", e -> {
            FirewallRuleModel rule = new FirewallRuleModel(
                    vpnType,
                    direction
            );
            editRule(rule);
        });
        addRule.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button saveAllRules = new Button("Apply all Rules", e -> {
            String fileName = "/openvpn-%s-firewall-rules.json".formatted(
                    vpnType.name().toLowerCase()
            );
            try {
                firewallController.writeRules(vpnType);
                ShowNotification.info("Configuration written to " + fileName);
            } catch (IOException | JSONException ex) {
                String msg = "Cannot write firewall rules to %s: %s"
                        .formatted(fileName, ex.getMessage());
                log.error(msg);
                ShowNotification.error("Error", msg);
            }
        });

        HorizontalLayout buttonsLayout = new HorizontalLayout(
                addRule,
                saveAllRules
        );

        add(grid, buttonsLayout);
        setHeightFull();
        setMargin(false);
        setPadding(false);

        grid.setItems(dataProvider);
    }

    private <T> Details createDetails(Collection<T> items) {
        UnorderedList detailItems = new UnorderedList();
        items.forEach((w) -> {
            detailItems.add(new ListItem(w.toString()));
        });
        detailItems.addClassNames(
                LumoUtility.Padding.NONE,
                LumoUtility.Margin.NONE
        );

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

    private void deleteRule(FirewallRuleModel rule) {
        grid.select(rule);
        ConfirmDialog dlg = new ConfirmDialog();
        dlg.setHeader("Delete Rule");
        dlg.setText(
                """
                Do you want to delete the selected firewall rule?
                This action cannot be undone.
                """
        );
        dlg.setCancelable(true);
        dlg.setConfirmText("Delete");
        dlg.addConfirmListener((e) -> {
            firewallRuleRepository.delete(rule);
            grid.getDataProvider().refreshAll();
        });

        dlg.open();
    }

    private void editRule(FirewallRuleModel rule) {
        final String TUPEL_WIDTH = "25em";

        Dialog dlg = new Dialog();
        if (rule.getId() == null) {
            dlg.setHeaderTitle("New rule");
        } else {
            grid.select(rule);
            dlg.setHeaderTitle("Edit rule");
        }

        Binder<FirewallRuleModel> binder = new Binder<>();
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setMargin(false);
        mainLayout.setPadding(false);
        HorizontalLayout ruleTiupelLayout = new HorizontalLayout();
        ruleTiupelLayout.setMargin(false);
        ruleTiupelLayout.setPadding(false);

        MagicEditableListBox<FirewallWho> who;
        Checkbox everybody;
        if (rule.getVpnType() == FirewallRuleModel.VpnType.USER) {
            who = new MagicEditableListBox<>(
                    FirewallWho.class,
                    "Who",
                    () -> new EditFirewallWho(userMatcherCollector, ldapSettings)
            );
            binder.forField(who)
                    .bind(FirewallRuleModel::getWho, FirewallRuleModel::setWho);

            everybody = new Checkbox(
                    "Everybody",
                    e -> who.setEnabled(!e.getValue())
            );
            everybody.setValue(
                    rule.getWho() != null
                    && rule.getWho().size() == 1
                    && rule.getWho().get(0)
                            .getUserMatcherClassName()
                            .equals(EverybodyMatcher.class.getName())
            );

            VerticalLayout vbox = new VerticalLayout(everybody, who);
            vbox.setWidth(TUPEL_WIDTH);
            vbox.setMargin(false);
            vbox.setPadding(false);
            ruleTiupelLayout.add(vbox);
        } else {
            everybody = null;
            who = null;
        }

        MagicEditableListBox<FirewallWhere> from;
        Checkbox fromEveryWhere;
        if (rule.getVpnType() == FirewallRuleModel.VpnType.SITE
                || rule.getRuleDirection() == FirewallRuleModel.RuleDirection.OUTGOING) {
            from = new MagicEditableListBox<>(
                    FirewallWhere.class,
                    "From",
                    () -> new EditFirewallWhere()
            );
            binder.forField(from)
                    .bind(FirewallRuleModel::getFrom, FirewallRuleModel::setFrom);

            fromEveryWhere = new Checkbox(
                    "From everyWhere",
                    e -> from.setEnabled(!e.getValue())
            );
            fromEveryWhere.setValue(
                    rule.getFrom() != null
                    && rule.getFrom().size() == 1
                    && rule.getFrom().get(0).getType() == FirewallWhere.Type.Everywhere
            );

            VerticalLayout vbox = new VerticalLayout(fromEveryWhere, from);
            vbox.setWidth(TUPEL_WIDTH);
            vbox.setMargin(false);
            vbox.setPadding(false);
            ruleTiupelLayout.add(vbox);
        } else {
            from = null;
            fromEveryWhere = null;
        }

        MagicEditableListBox<FirewallWhere> to;
        Checkbox toEveryWhere;
        if (rule.getVpnType() == FirewallRuleModel.VpnType.SITE
                || rule.getRuleDirection() == FirewallRuleModel.RuleDirection.INCOMING) {
            to = new MagicEditableListBox<>(
                    FirewallWhere.class,
                    "To",
                    () -> new EditFirewallWhere()
            );
            to.setItemRenderer(new ComponentRenderer<>(t -> {
                HorizontalLayout layout = new HorizontalLayout();
                layout.setMargin(false);
                layout.setPadding(false);

                Text label = new Text(t.toString());
                layout.addToStart(label);

                Div d = new Div(VaadinIcon.INFO_CIRCLE.create());
                Component info = t.createInfoPopover(d);
                if (info != null) {
                    layout.addToEnd(d, info);
                }

                return layout;
            }));
            binder.forField(to)
                    .bind(FirewallRuleModel::getTo, FirewallRuleModel::setTo);

            toEveryWhere = new Checkbox(
                    "To everywhere",
                    e -> to.setEnabled(!e.getValue())
            );
            toEveryWhere.setValue(
                    rule.getTo() != null
                    && rule.getTo().size() == 1
                    && rule.getTo().get(0).getType() == FirewallWhere.Type.Everywhere
            );

            VerticalLayout vbox = new VerticalLayout(toEveryWhere, to);
            vbox.setWidth(TUPEL_WIDTH);
            vbox.setMargin(false);
            vbox.setPadding(false);
            ruleTiupelLayout.add(vbox);
        } else {
            toEveryWhere = null;
            to = null;
        }

        MagicEditableListBox<FirewallWhat> what = new MagicEditableListBox<>(
                FirewallWhat.class,
                "What",
                () -> new EditFirewallWhat()
        );
        what.setItemRenderer(new ComponentRenderer<>(w -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setMargin(false);
            layout.setPadding(false);

            Text label = new Text(w.toString());
            layout.addToStart(label);

            Div infoButton = new Div(VaadinIcon.INFO_CIRCLE.create());
            Component info = w.createInfoPopover(infoButton);
            if (info != null) {
                layout.addToEnd(infoButton, info);
            }

            return layout;
        }));
        binder.forField(what)
                .bind(FirewallRuleModel::getWhat, FirewallRuleModel::setWhat);

        Checkbox everything = new Checkbox(
                "Everything",
                e -> what.setEnabled(!e.getValue())
        );
        everything.setValue(
                rule.getWhat() != null
                && rule.getWhat().size() == 1
                && rule.getWhat().get(0).getType() == FirewallWhat.Type.Everything
        );

        VerticalLayout vbox = new VerticalLayout(everything, what);
        vbox.setWidth(TUPEL_WIDTH);
        vbox.setMargin(false);
        vbox.setPadding(false);
        ruleTiupelLayout.add(vbox);

        TextField descriptionField = new TextField("Description");
        descriptionField.setWidthFull();
        descriptionField.setClearButtonVisible(true);
        binder.forField(descriptionField)
                .bind(FirewallRuleModel::getDescription, FirewallRuleModel::setDescription);

        Checkbox isEnabledField = new Checkbox("Enable Rule");
        binder.forField(isEnabledField)
                .bind(FirewallRuleModel::isEnabled, FirewallRuleModel::setEnabled);

        mainLayout.add(
                ruleTiupelLayout,
                descriptionField,
                isEnabledField
        );

        dlg.add(mainLayout);

        Button okButton = new Button("OK", (ClickEvent<Button> t) -> {
            dlg.close();

            if (who != null) {
                if (!everybody.getValue()) {
                    rule.setWho(who.getValue());
                } else {
                    if (rule.getWho().size() != 1
                            || !rule.getWho().get(0)
                                    .getUserMatcherClassName()
                                    .equals(EverybodyMatcher.class.getName())) {
                        rule.setWho(List.of(FirewallWho.createEverybody()));
                    }
                }
            }
            if (from != null) {
                if (!fromEveryWhere.getValue()) {
                    rule.setFrom(from.getValue());
                } else {
                    if (rule.getFrom().size() != 1
                            || rule.getFrom().get(0).getType() != FirewallWhere.Type.Everywhere) {
                        rule.setFrom(List.of(FirewallWhere.createEverywhere()));
                    }
                }
            }
            if (to != null) {
                if (!toEveryWhere.getValue()) {
                    rule.setTo(to.getValue());
                } else {
                    if (rule.getTo().size() != 1
                            || rule.getTo().get(0).getType() != FirewallWhere.Type.Everywhere) {
                        rule.setTo(List.of(FirewallWhere.createEverywhere()));
                    }
                }
            }
            if (!everything.getValue()) {
                rule.setWhat(what.getValue());
            } else {
                if (rule.getWhat().size() != 1
                        || rule.getWhat().get(0).getType() != FirewallWhat.Type.Everything) {
                    rule.setWhat(List.of(FirewallWhat.createEverything()));
                }
            }
            rule.setEnabled(isEnabledField.getValue());
            rule.setDescription(descriptionField.getValue());
            firewallRuleRepository.save(rule);
            grid.getDataProvider().refreshAll();
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", (t) -> {
            dlg.close();
        });

        dlg.getFooter().add(
                cancelButton,
                okButton
        );

        binder.setBean(rule);
        binder.validate();

        dlg.open();
    }
}
