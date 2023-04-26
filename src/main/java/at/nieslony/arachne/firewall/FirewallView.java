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
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

/**
 *
 * @author claas
 */
@Route(value = "firewall", layout = ViewTemplate.class)
@PageTitle("Firewall | Arachne")
@RolesAllowed("ADMIN")
public class FirewallView extends VerticalLayout {

    final private FirewallRuleRepository firewallRuleRepository;

    public FirewallView(FirewallRuleRepository firewallRuleRepository) {
        this.firewallRuleRepository = firewallRuleRepository;

        Grid<FirewallRuleModel> allowGrid = new Grid<>();
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
        Button addWhoButton = new Button("Add...");
        Button editWhoButton = new Button("Edit...");
        Button removeWhoButton = new Button("Remove");
        VerticalLayout editWho = new VerticalLayout(
                whoLabel,
                whoList,
                new HorizontalLayout(
                        addWhoButton,
                        editWhoButton,
                        removeWhoButton
                )
        );

        Label whereLabel = new Label("Where");
        ListBox<FirewallWhere> whereList = new ListBox<>();
        whereList.setHeight(30, Unit.EX);
        whereList.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        whereList.setWidthFull();
        Button addWhereButton = new Button("Add...");
        Button editWhereButton = new Button("Edit...");
        Button removeWhereButton = new Button("Remove");
        VerticalLayout editWhere = new VerticalLayout(
                whereLabel,
                whereList,
                new HorizontalLayout(
                        addWhereButton,
                        editWhereButton,
                        removeWhereButton
                )
        );

        Label whatLabel = new Label("What");
        ListBox<FirewallWhat> whatList = new ListBox<>();
        whatList.setHeight(30, Unit.EX);
        whatList.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        whatList.setWidthFull();
        Button addWhatButton = new Button("Add...");
        Button editWhatButton = new Button("Edit...");
        Button removeWhatButton = new Button("Remove");
        VerticalLayout editWhat = new VerticalLayout(
                whatLabel,
                whatList,
                new HorizontalLayout(
                        addWhatButton,
                        editWhatButton,
                        removeWhatButton
                )
        );

        TextField descriptionField = new TextField("Description");
        descriptionField.setWidthFull();
        descriptionField.setClearButtonVisible(true);
        Checkbox isEnabledField = new Checkbox("Enable Rule");

        dlg.add(new VerticalLayout(
                new HorizontalLayout(
                        editWho,
                        editWhere,
                        editWhat
                ),
                descriptionField,
                isEnabledField
        ));

        Button saveButton = new Button("Save", e -> {
            firewallRuleRepository.save(rule);
            dlg.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setAutofocus(true);

        Button cancelButton = new Button("Cancel", e -> dlg.close());

        dlg.getFooter().add(cancelButton);
        dlg.getFooter().add(saveButton);

        dlg.open();
    }

    private void editWho(FirewallWho who) {
        Dialog dlg = new Dialog();
        if (who.getId() == null) {
            dlg.setHeaderTitle("Add Who");
        } else {
            dlg.setHeaderTitle("Edit Who");
        }
    }
}
