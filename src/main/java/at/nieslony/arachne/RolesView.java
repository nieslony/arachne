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
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.VaadinIcon;
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
    Grid.Column<RoleRuleModel> ruleColumn;
    Grid.Column<RoleRuleModel> parameterColumn;
    Grid.Column<RoleRuleModel> roleColumn;
    Grid.Column<RoleRuleModel> descriptionColumn;

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
        ruleColumn = roleRules
                .addColumn(RoleRuleModel::getRoleRuleDescription)
                .setHeader("Rule");
        parameterColumn = roleRules
                .addColumn(RoleRuleModel::getParameter)
                .setHeader("Parameter");
        roleColumn = roleRules
                .addColumn(RoleRuleModel::getRoleReadable)
                .setHeader("Assigned Role");
        descriptionColumn = roleRules
                .addColumn(RoleRuleModel::getDescription)
                .setHeader("Description");

        roleRules.setItems(roleRuleRepository.findAll());

        editRoleBuffered();

        add(topButtons, roleRules);
    }

    void editRoleBuffered() {
        Editor<RoleRuleModel> editor = roleRules.getEditor();
        Binder<RoleRuleModel> binder = new Binder(RoleRuleModel.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

        Grid.Column<RoleRuleModel> editColumn = roleRules
                .addComponentColumn(roleRule -> {
                    Button editButton = new Button("Edit");
                    editButton.addClickListener(e -> {
                        if (editor.isOpen()) {
                            editor.cancel();
                        }
                        editor.editItem(roleRule);
                    });
                    return editButton;
                })
                .setWidth("10em")
                .setFlexGrow(0);

        Select<UserMatcherInfo> userMatchersField = new Select<>();
        List<UserMatcherInfo> allUserMatchers = rolesCollector.getAllUserMatcherInfo();
        userMatchersField.setItems(allUserMatchers);
        userMatchersField.setEmptySelectionAllowed(false);
        binder.forField(userMatchersField)
                .bind(
                        rr -> {
                            return new UserMatcherInfo(rr.getUserMatcherClassName());
                        },
                        (rr, v) -> {
                            rr.setUserMatcherClassName(v.getClassName());
                        }
                );
        ruleColumn.setEditorComponent(userMatchersField);

        TextField parameterField = new TextField();
        binder.forField(parameterField)
                .withValidator(
                        text -> {
                            String label = userMatchersField.getValue().getParameterLabel();
                            if (label == null || label.isEmpty()) {
                                return true;
                            }
                            return !parameterField.getValue().isEmpty();
                        },
                        "Value required")
                .bind(RoleRuleModel::getParameter, RoleRuleModel::setParameter);
        parameterColumn.setEditorComponent(parameterField);

        Select<Role> roles = new Select();
        roles.setItems(Role.values());
        roles.setEmptySelectionAllowed(false);
        binder.forField(roles)
                .bind(RoleRuleModel::getRole, RoleRuleModel::setRole);
        roleColumn.setEditorComponent(roles);

        TextField descriptionField = new TextField();
        binder.forField(descriptionField)
                .bind(RoleRuleModel::getDescription, RoleRuleModel::setDescription);
        descriptionColumn.setEditorComponent(descriptionField);

        editor.addSaveListener((event) -> {
            RoleRuleModel roleRule = event.getItem();
            roleRuleRepository.save(roleRule);
        });

        Button saveButton = new Button(
                "Save",
                e -> {
                    editor.save();
                }
        );
        Button cancelButton = new Button(
                VaadinIcon.CLOSE.create(),
                e -> editor.cancel());
        cancelButton.addThemeVariants(
                ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_ERROR);
        HorizontalLayout actions = new HorizontalLayout(
                saveButton,
                cancelButton
        );
        actions.setPadding(false);
        editColumn.setEditorComponent(actions);

        binder.addStatusChangeListener((event) -> {
            saveButton.setEnabled(!event.hasValidationErrors());
        });

        userMatchersField.addValueChangeListener(event -> {
            UserMatcherInfo umi = event.getValue();
            if (umi.getParameterLabel().isEmpty()) {
                parameterField.setEnabled(false);
            } else {
                parameterField.setEnabled(true);
            }
            binder.validate();
        });
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

        binder.forField(parameter)
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
