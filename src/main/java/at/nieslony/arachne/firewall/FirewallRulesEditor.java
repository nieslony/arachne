/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.utils.components.MagicEditableListBox;
import at.nieslony.arachne.utils.components.YesNoIcon;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
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
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.vaadin.firitin.layouts.HorizontalFloatLayout;

/**
 *
 * @author claas
 */
@Slf4j
class FirewallRulesEditor extends VerticalLayout {

    private final FirewallRuleRepository firewallRuleRepository;
    private final UserMatcherCollector userMatcherCollector;
    private final LdapSettings ldapSettings;

    private final Grid<FirewallRuleModel> grid;

    public FirewallRulesEditor(
            FirewallRuleRepository firewallRuleRepository,
            UserMatcherCollector userMatcherCollector,
            LdapSettings ldapSettings,
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
                    return layout;
                }))
                .setFlexGrow(0);

        Button addRule = new Button("Add...", e -> {
            FirewallRuleModel rule = new FirewallRuleModel(
                    vpnType,
                    direction
            );
            editRule(rule);
        });

        add(addRule, grid);

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
        Dialog dlg = new Dialog();
        if (rule.getId() == null) {
            dlg.setHeaderTitle("New rule");
        } else {
            grid.select(rule);
            dlg.setHeaderTitle("Edit rule");
        }

        Binder<FirewallRuleModel> binder = new Binder<>();

        HorizontalLayout horLayout = new HorizontalLayout();
        horLayout.setMargin(false);
        horLayout.setPadding(false);

        MagicEditableListBox<FirewallWho> who;
        if (rule.getVpnType() == FirewallRuleModel.VpnType.USER) {
            who = new MagicEditableListBox<>(
                    FirewallWho.class,
                    "Who",
                    () -> new EditFirewallWho(userMatcherCollector, ldapSettings)
            );
            who.setMinWidth(25, Unit.EM);
            binder.forField(who)
                    .bind(FirewallRuleModel::getWho, FirewallRuleModel::setWho);
            horLayout.add(who);
        } else {
            who = null;
        }

        MagicEditableListBox<FirewallWhere> from;
        if (rule.getVpnType() == FirewallRuleModel.VpnType.SITE
                || rule.getRuleDirection() == FirewallRuleModel.RuleDirection.OUTGOING) {
            from = new MagicEditableListBox<>(
                    FirewallWhere.class,
                    "From",
                    () -> new EditFirewallWhere()
            );
            from.setMinWidth(25, Unit.EM);
            binder.forField(from)
                    .bind(FirewallRuleModel::getFrom, FirewallRuleModel::setFrom);
            horLayout.add(from);
        } else {
            from = null;
        }

        MagicEditableListBox<FirewallWhere> to;
        if (rule.getVpnType() == FirewallRuleModel.VpnType.SITE
                || rule.getRuleDirection() == FirewallRuleModel.RuleDirection.INCOMING) {
            to = new MagicEditableListBox<>(
                    FirewallWhere.class,
                    "To",
                    () -> new EditFirewallWhere()
            );
            to.setMinWidth(25, Unit.EM);
            to.setItemRenderer(new ComponentRenderer<>(t -> {
                HorizontalLayout layout = new HorizontalFloatLayout();
                layout.setMargin(false);
                layout.setPadding(false);
                layout.setAlignItems(Alignment.CENTER);

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
            horLayout.add(to);
        } else {
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
            layout.setAlignItems(Alignment.CENTER);

            Text label = new Text(w.toString());
            layout.addToStart(label);

            Div infoButton = new Div(VaadinIcon.INFO_CIRCLE.create());
            Component info = w.createInfoPopover(infoButton);
            if (info != null) {
                layout.addToEnd(infoButton, info);
            }

            return layout;
        }));
        what.setMinWidth(25, Unit.EM);
        binder.forField(what)
                .bind(FirewallRuleModel::getWhat, FirewallRuleModel::setWhat);
        horLayout.add(what);

        TextField descriptionField = new TextField("Description");
        descriptionField.setWidthFull();
        descriptionField.setClearButtonVisible(true);
        binder.forField(descriptionField)
                .bind(FirewallRuleModel::getDescription, FirewallRuleModel::setDescription);

        Checkbox isEnabledField = new Checkbox("Enable Rule");
        binder.forField(isEnabledField)
                .bind(FirewallRuleModel::isEnabled, FirewallRuleModel::setEnabled);

        VerticalLayout layout = new VerticalLayout(
                horLayout,
                descriptionField,
                isEnabledField
        );
        layout.setMargin(false);
        layout.setPadding(false);
        dlg.add(layout);

        Button okButton = new Button("OK", (t) -> {
            dlg.close();

            if (who != null) {
                rule.setWho(who.getValue());
            }
            if (from != null) {
                rule.setFrom(from.getValue());
            }
            if (to != null) {
                rule.setTo(to.getValue());
            }
            rule.setWhat(what.getValue());
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

        dlg.open();
    }
}
