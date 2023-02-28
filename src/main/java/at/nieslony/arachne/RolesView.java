/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.roles.Role;
import at.nieslony.arachne.roles.RoleRuleModel;
import at.nieslony.arachne.roles.RoleRuleRepository;
import at.nieslony.arachne.roles.RolesCollector;
import at.nieslony.arachne.users.UserMatcherInfo;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "roles", layout = ViewTemplate.class)
@PageTitle("Roles | Arachne")
@RolesAllowed("ADMIN")
public class RolesView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(RolesView.class);

    final private RoleRuleRepository roleRuleRepository;
    final private RolesCollector rolesCollector;

    final Grid<RoleRuleModel> roleRules;

    public RolesView(
            RoleRuleRepository roleRuleRepository,
            RolesCollector rolesCollector
    ) {
        this.roleRuleRepository = roleRuleRepository;
        this.rolesCollector = rolesCollector;

        Button addRole = new Button("Add...", e -> {
            addRule();
        });

        HorizontalLayout topButtons = new HorizontalLayout();
        topButtons.add(addRole);

        roleRules = new Grid();
        roleRules
                .addColumn(RoleRuleModel::getRoleRuleDescription)
                .setHeader("Rule");
        roleRules
                .addColumn(RoleRuleModel::getParameter)
                .setHeader("Parameter");
        roleRules
                .addColumn(RoleRuleModel::getRoleReadable)
                .setHeader("Assigned Role");
        roleRules
                .addColumn(RoleRuleModel::getDescription)
                .setHeader("Description");

        roleRules.setItems(roleRuleRepository.findAll());

        add(topButtons, roleRules);
    }

    void addRule() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Role Rule");
        Binder<RoleRuleModel> binder = new Binder<>(RoleRuleModel.class);

        Button okButton = new Button("OK", e -> {
            dialog.close();
            RoleRuleModel roleRule = new RoleRuleModel();
            binder.writeBeanIfValid(roleRule);

            roleRuleRepository.save(roleRule);
            roleRules.setItems(roleRuleRepository.findAll());
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelButton = new Button("Cancel", e -> {
            dialog.close();
        });
        binder.addStatusChangeListener((event) -> {
            okButton.setEnabled(!event.hasValidationErrors());
        });
        RoleRuleModel roleRule = new RoleRuleModel();

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(okButton);

        Select<UserMatcherInfo> userMatchers = new Select<>();
        List<UserMatcherInfo> allUserMatchers = rolesCollector.getAllUserMatcherInfo();
        userMatchers.setItems(allUserMatchers);
        userMatchers.setEmptySelectionAllowed(false);
        userMatchers.setLabel("Role Rules");
        userMatchers.setEmptySelectionAllowed(false);

        TextField parameter = new TextField("Parameter");

        Select<Role> roles = new Select();
        roles.setLabel(("Role"));
        Role[] allRoles = Role.values();
        roles.setItems(Role.values());
        roles.setEmptySelectionAllowed(false);

        TextField description = new TextField("Description");

        userMatchers.setValue(allUserMatchers.get(0));
        roles.setValue(allRoles[0]);

        binder.forField(userMatchers)
                .bind(
                        rr -> {
                            return new UserMatcherInfo(rr.getUserMatcherClassName());
                        },
                        (rr, v) -> {
                            rr.setUserMatcherClassName(v.getClassName());
                        }
                );

        var parameterBinder = binder.forField(parameter)
                .withValidator(
                        text -> {
                            String label = userMatchers.getValue().getParameterLabel();
                            if (label == null || label.isEmpty()) {
                                return true;
                            }
                            return !parameter.getValue().isEmpty();
                        },
                        "Value required")
                .bind(RoleRuleModel::getParameter, RoleRuleModel::setParameter);

        binder.forField(roles)
                .bind(RoleRuleModel::getRole, RoleRuleModel::setRole);

        binder.forField(description)
                .bind(RoleRuleModel::getDescription, RoleRuleModel::setDescription);

        userMatchers.addValueChangeListener(event -> {
            UserMatcherInfo umi = event.getValue();
            if (umi.getParameterLabel().isEmpty()) {
                parameter.setEnabled(false);
                parameter.setLabel("Without parameter");
            } else {
                parameter.setEnabled(true);
                parameter.setLabel(umi.getParameterLabel());
            }
            binder.validate();
        });

        binder.validate();

        dialog.add(new FormLayout(
                userMatchers,
                parameter,
                roles,
                description
        ));

        dialog.open();
    }
}
