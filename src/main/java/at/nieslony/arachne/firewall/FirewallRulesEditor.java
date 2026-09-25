/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.ldap.LdapService;
import at.nieslony.arachne.usermatcher.UserMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.users.UserRepository;
import at.nieslony.arachne.utils.components.LdapAutoComplete;
import at.nieslony.arachne.utils.components.ShowNotification;
import at.nieslony.arachne.utils.components.YesNoIcon;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializablePredicate;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.ObjectUtils;

/**
 *
 * @author claas
 */
@Slf4j
class FirewallRulesEditor extends VerticalLayout {

    private final FirewallRuleRepository firewallRuleRepository;
    private final UserMatcherCollector userMatcherCollector;
    private final LdapService ldapService;
    private final UserRepository userRepository;

    private final Grid<FirewallRuleModel> grid;
    private final Text firewallAction;
    private final BeanFactory beanFactory;
    private final ListDataProvider<FirewallRuleModel> dataProvider;
    private final FirewallRuleModel.VpnType vpnType;
    private final FirewallRuleModel.RuleDirection direction;

    private AtomicBoolean firewallRestartRequired = new AtomicBoolean(false);

    public FirewallRulesEditor(
            BeanFactory beanFactory,
            FirewallRuleModel.VpnType vpnType,
            FirewallRuleModel.RuleDirection direction
    ) {
        this.beanFactory = beanFactory;
        this.firewallRuleRepository = beanFactory.getBean(FirewallRuleRepository.class);
        this.userMatcherCollector = beanFactory.getBean(UserMatcherCollector.class);
        this.ldapService = beanFactory.getBean(LdapService.class);
        this.userRepository = beanFactory.getBean(UserRepository.class);
        this.vpnType = vpnType;
        this.direction = direction;

        FirewallService firewallService = beanFactory.getBean(FirewallService.class);

        dataProvider = new ListDataProvider<>(
                firewallRuleRepository.findAllByVpnTypeAndRuleDirection(
                        vpnType,
                        direction
                ));

        TextField filterByUser = new TextField("Find Rules matching User");
        filterByUser.setMinWidth(30, Unit.REM);
        filterByUser.setClearButtonVisible(true);

        LdapAutoComplete findUser = new LdapAutoComplete(filterByUser, ldapService);
        findUser.setCompleteMode(LdapAutoComplete.CompleteMode.USERS);

        Button filterByUserButton = new Button(
                VaadinIcon.SEARCH.create(),
                (e) -> dataProvider.setFilter(
                        createFilter(filterByUser.getValue())
                )
        );

        HorizontalLayout filterLayout = new HorizontalLayout(
                filterByUser,
                findUser,
                filterByUserButton
        );
        filterLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        filterLayout.setWidthFull();
        filterLayout.setVisible(vpnType == FirewallRuleModel.VpnType.USER);

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
        addRule.addThemeVariants(ButtonVariant.PRIMARY);

        Button saveAllRules = new Button("Apply all Rules", e -> {
            String fileName = "/openvpn-%s-firewall-rules.json".formatted(
                    vpnType.name().toLowerCase()
            );
            try {
                firewallService.writeRules(vpnType);
                ShowNotification.info("Configuration written to " + fileName);
            } catch (IOException | JSONException ex) {
                String msg = "Cannot write firewall rules to %s: %s"
                        .formatted(fileName, ex.getMessage());
                log.error(msg);
                ShowNotification.error("Error", msg);
            }
        });
        firewallAction = new Text("");

        HorizontalLayout buttonsLayout = new HorizontalLayout(
                addRule,
                saveAllRules,
                firewallAction
        );

        add(
                filterLayout,
                grid,
                buttonsLayout
        );
        setHeightFull();
        setMargin(false);
        setPadding(false);

        grid.setDataProvider(dataProvider);
    }

    private SerializablePredicate<FirewallRuleModel> createFilter(String username) {
        if (ObjectUtils.isEmpty(username)) {
            return frm -> true;
        }

        return (FirewallRuleModel frm) -> {
            if (!frm.isEnabled()) {
                return false;
            }
            var user = userRepository.findByUsername(username);
            if (user == null) {
                return false;
            }
            for (FirewallWho who : frm.getWho()) {
                if (who.isEverybody()) {
                    return true;
                }
                UserMatcher matcher = userMatcherCollector.buildUserMatcher(
                        who.getUserMatcherClassName(),
                        who.getParameter()
                );
                if (matcher.isUserMatching(user)) {
                    return true;
                }
            }
            return false;
        };
    }

    private <T> Details createDetails(Collection<T> items) {
        UnorderedList detailItems = new UnorderedList();
        items.forEach((w) -> {
            detailItems.add(new ListItem(w.toString()));
        });

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
            updateDataProvider();
            grid.getDataProvider().refreshAll();
        });

        dlg.open();
    }

    private void updateDataProvider() {
        dataProvider.getItems().clear();
        dataProvider.getItems().addAll(
                firewallRuleRepository.findAllByVpnTypeAndRuleDirection(
                        vpnType,
                        direction
                ));
    }

    private void editRule(FirewallRuleModel rule) {
        EditFirewallRule editFirewallRule = new EditFirewallRule(
                rule,
                r -> {
                    firewallRuleRepository.save(r);
                    updateDataProvider();
                    grid.getDataProvider().refreshAll();
                    firewallAction.setText(getFirewallActionText());
                },
                beanFactory
        );

        editFirewallRule.open();
    }

    private String getFirewallActionText() {
        if (firewallRestartRequired.get()) {
            return "Firewall Restart required";
        }

        return "";
    }
}
