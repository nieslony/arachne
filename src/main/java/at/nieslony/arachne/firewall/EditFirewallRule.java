/*
 * Copyright (C) 2026 claas
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

import at.nieslony.arachne.ldap.LdapService;
import at.nieslony.arachne.usermatcher.EverybodyMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.utils.components.MagicEditableListBox;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
@Slf4j
public class EditFirewallRule extends Dialog {

    private final String TUPEL_WIDTH = "25em";

    private Consumer<FirewallRuleModel> onOk;

    public EditFirewallRule(
            FirewallRuleModel rule,
            Consumer<FirewallRuleModel> onOk,
            BeanFactory beanFactory
    ) {
        setDraggable(true);
        if (rule.getId() == null) {
            setHeaderTitle("New rule");
        } else {
            setHeaderTitle("Edit rule");
        }
        this.onOk = onOk;

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
                    () -> new EditFirewallWho(
                            beanFactory.getBean(UserMatcherCollector.class),
                            beanFactory.getBean(LdapService.class)
                    )
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

        add(mainLayout);

        Button okButton = new Button("OK", (ClickEvent<Button> t) -> {
            close();

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
            log.info("what.value: " + what.getValue().toString());
            log.info("rule.what: " + rule.getWhat().toString());
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

            onOk.accept(rule);
        });
        okButton.addThemeVariants(ButtonVariant.PRIMARY);

        Button cancelButton = new Button("Cancel", (t) -> {
            close();
        });

        getFooter().add(
                cancelButton,
                okButton
        );

        binder.setBean(rule);
        binder.validate();
    }

}
