/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.roles.Role;
import at.nieslony.arachne.roles.RoleRuleModel;
import at.nieslony.arachne.roles.RoleRuleRepository;
import at.nieslony.arachne.roles.RolesCollector;
import at.nieslony.arachne.users.UserMatcher;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import javax.annotation.security.RolesAllowed;

/**
 *
 * @author claas
 */
@Route(value = "roles", layout = ViewTemplate.class)
@PageTitle("Roles | Arachne")
@RolesAllowed("ADMIN")
public class RolesView extends VerticalLayout {

    final private RoleRuleRepository roleRuleRepository;
    final private RolesCollector rolesCollector;

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

        Grid<RoleRuleModel> roleRules = new Grid();
        roleRules
                .addColumn(RoleRuleModel::getRoleRuleDescription)
                .setHeader("Rule");
        roleRules
                .addColumn(RoleRuleModel::getParameter)
                .setHeader("Parameter");
        roleRules
                .addColumn(RoleRuleModel::getRoleReadable)
                .setHeader("Attached Role");
        roleRules
                .addColumn(RoleRuleModel::getDescription)
                .setHeader("Description");

        roleRules.setItems(roleRuleRepository.findAll());

        add(topButtons, roleRules);
    }

    void addRule() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Role Rule");

        Select<Class<? extends UserMatcher>> userMatchers = new Select<>();
        List<Class<? extends UserMatcher>> allUserMatchers = rolesCollector.getUserMatcherClasses();
        userMatchers.setItems(allUserMatchers);
        userMatchers.setLabel("Role Rules");

        TextField parameter = new TextField("Parameter");

        Select<Role> roles = new Select();
        roles.setLabel(("Role"));
        roles.setItems(Role.values());

        TextField description = new TextField("Description");

        dialog.add(new FormLayout(
                userMatchers,
                parameter,
                roles,
                description
        ));

        Button okButton = new Button("OK", e -> {
            dialog.close();
        });
        Button cancelButton = new Button("Cancel", e -> {
            dialog.close();
        });
        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(okButton);

        dialog.open();
    }
}
