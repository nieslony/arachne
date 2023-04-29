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
import at.nieslony.arachne.usermatcher.EverybodyMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.usermatcher.UserMatcherInfo;
import at.nieslony.arachne.utils.TransportProtocol;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "firewall", layout = ViewTemplate.class)
@PageTitle("Firewall | Arachne")
@RolesAllowed("ADMIN")
public class FirewallView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(FirewallView.class);

    final private FirewallRuleRepository firewallRuleRepository;
    final private UserMatcherCollector userMatcherCollector;

    Grid<FirewallRuleModel> allowGrid;

    public FirewallView(
            FirewallRuleRepository firewallRuleRepository,
            UserMatcherCollector userMatcherCollector
    ) {
        this.firewallRuleRepository = firewallRuleRepository;
        this.userMatcherCollector = userMatcherCollector;

        allowGrid = new Grid<>();
        allowGrid
                .addColumn(new ComponentRenderer<>(
                        (var model) -> {
                            return new Text(model.getDescription());
                        }
                ))
                .setHeader("Who");

        allowGrid
                .addColumn(new ComponentRenderer<>(
                        (var model) -> {
                            return new Text(model.getDescription());
                        }
                ))
                .setHeader("Where");

        allowGrid
                .addColumn(new ComponentRenderer<>(
                        (var model) -> {
                            return new Text(model.getDescription());
                        }
                ))
                .setHeader("What");

        allowGrid
                .addColumn(FirewallRuleModel::getDescription)
                .setHeader("Description");

        Button addRule = new Button("Add...", e -> {
            FirewallRuleModel rule = new FirewallRuleModel();
            editRule(rule);
        });

        add(addRule, allowGrid);
    }

    private void editRule(FirewallRuleModel rule) {
        Dialog dlg = new Dialog();
        if (rule.getId() == null) {
            dlg.setHeaderTitle("New rule");
        } else {
            dlg.setHeaderTitle("Edit rule");
        }

        Binder<FirewallRuleModel> binder = new Binder();
        binder.setBean(rule);

        Label whoLabel = new Label("Who");
        ListBox<FirewallWho> whoList = new ListBox<>();
        whoList.setHeight(30, Unit.EX);
        whoList.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        whoList.setWidthFull();
        Button addWhoButton = new Button("Add...", (ClickEvent<Button> e) -> {
            FirewallWho who = new FirewallWho();
            who.setUserMatcherClassName(EverybodyMatcher.class.getName());
            editWho(who, (FirewallWho w) -> {
                List<FirewallWho> l = rule.getWho();
                if (l == null) {
                    l = new LinkedList<>();
                    rule.setWho(l);
                }
                l.add(w);
                whoList.setItems(l);
            });
        });
        Button editWhoButton = new Button("Edit...", (e) -> {
            FirewallWho who = whoList.getValue();
            editWho(who, (FirewallWho w) -> {
                List<FirewallWho> l = rule.getWho();
                whoList.setItems(l);
            });
        });
        editWhoButton.setEnabled(false);
        Button removeWhoButton = new Button("Remove", (e) -> {
            FirewallWho who = whoList.getValue();
            List<FirewallWho> l = rule.getWho();
            l.remove(who);
            whoList.setItems(l);
        });
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

        Label whereLabel = new Label("Where");
        ListBox<FirewallWhere> whereList = new ListBox<>();
        whereList.setHeight(30, Unit.EX);
        whereList.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        whereList.setWidthFull();
        Button addWhereButton = new Button("Add...", (e) -> {
            FirewallWhere where = new FirewallWhere();
            editWhere(where, (FirewallWhere w) -> {
                List<FirewallWhere> l = rule.getWhere();
                if (l == null) {
                    l = new LinkedList<>();
                    rule.setWhere(l);
                }
                l.add(w);
                whereList.setItems(l);
            });
        });
        Button editWhereButton = new Button("Edit...", (e) -> {
            FirewallWhere where = whereList.getValue();
            editWhere(where, (FirewallWhere w) -> {
                List<FirewallWhere> l = rule.getWhere();
                whereList.setItems(l);
            });
        });
        editWhereButton.setEnabled(false);
        Button removeWhereButton = new Button("Remove", (e) -> {
            FirewallWhere where = whereList.getValue();
            List<FirewallWhere> l = rule.getWhere();
            l.remove(where);
            whereList.setItems(l);
        });
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

        Label whatLabel = new Label("What");
        ListBox<FirewallWhat> whatList = new ListBox<>();
        whatList.setHeight(30, Unit.EX);
        whatList.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        whatList.setWidthFull();
        Button addWhatButton = new Button("Add...", (e) -> {
            FirewallWhat what = new FirewallWhat();
            editWhat(what, (w) -> {
                List<FirewallWhat> l = rule.getWhat();
                if (l == null) {
                    l = new LinkedList<>();
                    rule.setWhat(l);
                }
                l.add(w);
                whatList.setItems(l);
            });
        });
        Button editWhatButton = new Button("Edit...", (e) -> {
            FirewallWhat what = whatList.getValue();
            editWhat(what, (FirewallWhat w) -> {
                List<FirewallWhat> l = rule.getWhat();
                whatList.setItems(l);
            });
        });
        editWhatButton.setEnabled(false);
        Button removeWhatButton = new Button("Remove", (e) -> {
            FirewallWhat what = whatList.getValue();
            List<FirewallWhat> l = rule.getWhat();
            l.remove(what);
            whatList.setItems(l);
        });
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
        Checkbox isEnabledField = new Checkbox("Enable Rule");

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
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setAutofocus(true);

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

        dlg.getFooter().add(cancelButton, saveButton);
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

        Binder<FirewallWho> binder = new Binder<>();

        Select<UserMatcherInfo> userMatchersSelect = new Select<>();
        userMatchersSelect.setLabel("User Matcher");
        userMatchersSelect.setItems(userMatcherCollector.getAllUserMatcherInfo());
        userMatchersSelect.setEmptySelectionAllowed(false);
        binder.forField(userMatchersSelect)
                .bind(
                        rr -> {
                            return new UserMatcherInfo(rr.getUserMatcherClassName());
                        },
                        (rr, v) -> {
                            rr.setUserMatcherClassName(v.getClassName());
                        }
                );

        TextField parameterField = new TextField("Parameter");
        binder.forField(parameterField)
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

        userMatchersSelect.addValueChangeListener(
                (e) -> {
                    String labelTxt = e.getValue().getParameterLabel();
                    parameterField.setLabel(labelTxt);
                    parameterField.setVisible(labelTxt != null && !labelTxt.isEmpty());

                    binder.validate();
                }
        );

        dlg.add(new VerticalLayout(
                userMatchersSelect,
                parameterField
        ));

        Button saveButton = new Button("Save", (t) -> {
            dlg.close();
            logger.info(who.toString());
            onSave.accept(who);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setAutofocus(true);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        binder.addStatusChangeListener((sce) -> {
            saveButton.setEnabled(!sce.hasValidationErrors());
        });

        binder.setBean(who);
        binder.validate();

        dlg.getFooter().add(cancelButton, saveButton);
        dlg.open();
    }

    private void editWhere(FirewallWhere where, Consumer<FirewallWhere> onSave) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Edit Where");

        Binder<FirewallWhere> binder = new Binder();

        Select<FirewallWhere.Type> whereTypeSelect = new Select<>();
        whereTypeSelect.setLabel("Where Type");
        whereTypeSelect.setItems(FirewallWhere.Type.values());
        whereTypeSelect.setEmptySelectionAllowed(false);
        binder.forField(whereTypeSelect)
                .bind(FirewallWhere::getType, FirewallWhere::setType);

        TextField hostnameField = new TextField("Hostname");
        hostnameField.setWidthFull();
        hostnameField.setVisible(false);
        binder.forField(hostnameField)
                .bind(FirewallWhere::getHostname, FirewallWhere::setHostname);

        TextField networkField = new TextField("Network");
        networkField.setPattern("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
        binder.forField(networkField)
                .bind(FirewallWhere::getSubnet, FirewallWhere::setSubnet);

        IntegerField netMaskField = new IntegerField();
        netMaskField.setValue(32);
        netMaskField.setMin(1);
        netMaskField.setMax(32);
        netMaskField.setWidth(6, Unit.EM);
        netMaskField.setStepButtonsVisible(true);
        binder.forField(netMaskField)
                .bind(FirewallWhere::getSubnetMask, FirewallWhere::setSubnetMask);

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
        serviceRecDomainField.setPattern(("[a-z][a-z9-9\\-]*(\\.[a-z][a-z0-9\\-]*)*"));
        binder.forField(serviceRecDomainField)
                .bind(FirewallWhere::getServicerecDomain, FirewallWhere::setServicerecDomain);

        TextField serviceRecNameField = new TextField("Service");
        serviceRecNameField.setWidth(10, Unit.EM);
        serviceRecNameField.setPattern("[a-z]*");
        binder.forField(serviceRecNameField)
                .bind(FirewallWhere::getServiceRecName, FirewallWhere::setServiceRecName);

        Select<TransportProtocol> serviceRecProtocolField = new Select<>();
        serviceRecProtocolField.setLabel("Protocol");
        serviceRecProtocolField.setItems(TransportProtocol.values());
        serviceRecProtocolField.setWidth(6, Unit.EM);
        serviceRecProtocolField.setEmptySelectionAllowed(false);
        binder.forField(serviceRecProtocolField)
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

        VerticalLayout layout = new VerticalLayout(
                whereTypeSelect,
                hostnameField,
                networkEdit,
                serviceRecEdit
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
            switch (e.getValue()) {
                case Hostname ->
                    hostnameField.setVisible(true);
                case Subnet ->
                    networkEdit.setVisible(true);
                case ServiceRecord ->
                    serviceRecEdit.setVisible(true);
            }
        });

        dlg.getFooter().add(cancelButton, saveButton);

        binder.setBean(where);
        binder.validate();

        dlg.setMinWidth(40, Unit.EM);
        dlg.open();
    }

    private void editWhat(FirewallWhat what, Consumer<FirewallWhat> onSave) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Edit What");

        Binder<FirewallWhat> binder = new Binder<>();
        Collection<FirewalldService> firewalldServices = FirewalldService.getAllServices();

        Select<FirewallWhat.Type> whatTypeSelect = new Select<>();
        whatTypeSelect.setLabel("What Type");
        whatTypeSelect.setItems(FirewallWhat.Type.values());
        binder.forField(whatTypeSelect)
                .bind(FirewallWhat::getType, FirewallWhat::setType);

        ComboBox<FirewalldService> firewalldServiceSelect = new ComboBox<>();
        firewalldServiceSelect.setLabel("Firewalld Service");
        firewalldServiceSelect.setItems(firewalldServices);
        firewalldServiceSelect.setItemLabelGenerator(FirewalldService::getShortDescription);
        firewalldServiceSelect.setWidthFull();
        firewalldServiceSelect.setVisible(false);
        binder.forField(firewalldServiceSelect)
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
        binder.forField(portField)
                .bind(FirewallWhat::getPort, FirewallWhat::setPort);

        Select<TransportProtocol> portProtocolSelect = new Select<>();
        portProtocolSelect.setItems(TransportProtocol.values());
        binder.forField(portProtocolSelect)
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
        binder.forField(portFromField)
                .bind(FirewallWhat::getPortFrom, FirewallWhat::setPortFrom);

        IntegerField portToField = new IntegerField("Port to");
        portToField.setMin(1);
        portToField.setMax(65535);
        portToField.setWidth(8, Unit.EM);
        portToField.setStepButtonsVisible(true);
        binder.forField(portToField)
                .bind(FirewallWhat::getPortTo, FirewallWhat::setPortTo);

        Select<TransportProtocol> portRangeProtocolSelect = new Select<>();
        portRangeProtocolSelect.setItems(TransportProtocol.values());
        binder.forField(portRangeProtocolSelect)
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

        binder.setBean(what);
        binder.validate();

        dlg.open();
    }
}
