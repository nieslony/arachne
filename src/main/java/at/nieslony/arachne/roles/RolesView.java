/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.roles;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.ldap.LdapService;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.usermatcher.LdapGroupUserMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.usermatcher.UserMatcherInfo;
import at.nieslony.arachne.usermatcher.UsernameMatcher;
import at.nieslony.arachne.utils.components.LdapAutoComplete;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Route(value = "roles", layout = ViewTemplate.class)
@PageTitle("Roles")
@RolesAllowed("ADMIN")
@Slf4j
public class RolesView extends VerticalLayout {

    final private RoleRuleRepository roleRuleRepository;
    final private UserMatcherCollector userMatcherCollector;
    final private LdapService ldapService;

    final Grid<RoleRuleModel> roleRules;
    Grid.Column<RoleRuleModel> ruleColumn;
    Grid.Column<RoleRuleModel> parameterColumn;
    Grid.Column<RoleRuleModel> roleColumn;
    Grid.Column<RoleRuleModel> descriptionColumn;
    MasterDetailLayout masterDetailLayout;

    public RolesView(
            RoleRuleRepository roleRuleRepository,
            UserMatcherCollector userMatcherCollector,
            Settings settings,
            LdapService ldapService
    ) {
        this.roleRuleRepository = roleRuleRepository;
        this.userMatcherCollector = userMatcherCollector;
        this.ldapService = ldapService;

        Button addRole = new Button("Add...", e -> {
            addRule();
        });
        addRole.addThemeVariants(ButtonVariant.PRIMARY);

        HorizontalLayout topButtons = new HorizontalLayout();
        topButtons.add(addRole);

        roleRules = new Grid<>();
        ruleColumn = roleRules
                .addColumn(RoleRuleModel::getRoleRuleDescription)
                .setHeader("Rule")
                .setAutoWidth(true)
                .setFlexGrow(0);
        parameterColumn = roleRules
                .addColumn(RoleRuleModel::getParameter)
                .setHeader("Parameter")
                .setAutoWidth(true)
                .setFlexGrow(0);
        roleColumn = roleRules
                .addColumn(RoleRuleModel::getRoleReadable)
                .setHeader("Assigned Role")
                .setAutoWidth(true)
                .setFlexGrow(0);
        descriptionColumn = roleRules
                .addColumn(RoleRuleModel::getDescription)
                .setHeader("Description")
                .setFlexGrow(1);
        roleRules
                .addComponentColumn(roleRule -> {
                    MenuBar menuBar = new MenuBar();
                    menuBar.addItem("Edit", e -> {
                        roleRules.select(roleRule);
                        masterDetailLayout.setDetail(createEditRuleRule(roleRule));
                    });

                    SubMenu subMenu = menuBar.addItem("").getSubMenu();
                    subMenu.addItem("Delete…", e -> {
                    });
                    HorizontalLayout layout = new HorizontalLayout();
                    layout.addToEnd(menuBar);
                    layout.setPadding(false);
                    layout.setMargin(false);
                    return layout;
                })
                .setWidth("10em")
                .setFlexGrow(0);
        roleRules.setSizeFull();
        roleRules.setItems(roleRuleRepository.findAll());

        //editRoleBuffered();
        masterDetailLayout = new MasterDetailLayout();
        masterDetailLayout.setMaster(roleRules);
        masterDetailLayout.setMasterSize("50em", true);
        masterDetailLayout.setSizeFull();

        add(topButtons, masterDetailLayout);
        setPadding(false);
    }

    private Component createEditRuleRule(RoleRuleModel model) {
        Binder<RoleRuleModel> binder = new Binder<>();

        Select<UserMatcherInfo> userMatchersField = new Select<>();
        List<UserMatcherInfo> allUserMatchers = userMatcherCollector.getAllUserMatcherInfo();
        userMatchersField.setItems(allUserMatchers);
        userMatchersField.setEmptySelectionAllowed(false);
        userMatchersField.setLabel("Role Rule Type");
        userMatchersField.setWidthFull();
        binder.forField(userMatchersField)
                .bind(
                        rr -> {
                            return new UserMatcherInfo(rr.getUserMatcherClassName());
                        },
                        (rr, v) -> {
                            rr.setUserMatcherClassName(v.getClassName());
                        }
                );

        TextField parameterField = new TextField("Parameter");
        parameterField.setWidthFull();
        parameterField.setEnabled(false);
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
        LdapAutoComplete parameterFieldComplete = new LdapAutoComplete(
                parameterField,
                ldapService
        );

        Select<Role> roleSelect = new Select<>();
        roleSelect.setLabel("Role");
        roleSelect.setItems(Role.values());
        roleSelect.setWidthFull();
        binder.forField(roleSelect)
                .bind(RoleRuleModel::getRole, RoleRuleModel::setRole);

        TextField descriptionField = new TextField("Description");
        descriptionField.setWidthFull();
        binder.forField(descriptionField)
                .bind(RoleRuleModel::getDescription, RoleRuleModel::setDescription);

        Button closeButton = new Button("Close", e -> {
            masterDetailLayout.setDetail(null);
            roleRules.select(null);
        });
        Button saveButton = new Button(
                "Save",
                e -> {
                    roleRuleRepository.save(binder.getBean());
                    roleRules.setItems(roleRuleRepository.findAll());
                    masterDetailLayout.setDetail(null);
                    roleRules.select(null);
                }
        );
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.addToEnd(closeButton, saveButton);

        VerticalLayout layout = new VerticalLayout(
                userMatchersField,
                new HorizontalLayout(parameterField, parameterFieldComplete),
                roleSelect,
                descriptionField,
                buttonLayout
        );

        userMatchersField.addValueChangeListener(event -> {
            UserMatcherInfo umi = event.getValue();
            if (umi.getParameterLabel() == null || umi.getParameterLabel().isEmpty()) {
                parameterField.setEnabled(false);
                parameterField.setLabel("Without parameter");
            } else {
                parameterField.setEnabled(true);
                parameterField.setLabel(umi.getParameterLabel());
            }
            String className = umi.getClassName();
            if (className.equals(UsernameMatcher.class.getName())) {
                parameterFieldComplete.setCompleteMode(LdapAutoComplete.CompleteMode.USERS);
            } else if (className.equals(LdapGroupUserMatcher.class.getName())) {
                parameterFieldComplete.setCompleteMode(LdapAutoComplete.CompleteMode.GROUPS);
            } else {
                parameterFieldComplete.setCompleteMode(LdapAutoComplete.CompleteMode.NULL);
            }

            binder.validate();
        });

        binder.setBean(model);

        return layout;
    }

    void addRule() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Role Rule");
        dialog.setDraggable(true);
        Binder<RoleRuleModel> binder = new Binder<>(RoleRuleModel.class);

        Button okButton = new Button("OK", e -> {
            dialog.close();
            RoleRuleModel roleRule = new RoleRuleModel();
            binder.writeBeanIfValid(roleRule);

            roleRuleRepository.save(roleRule);
            roleRules.setItems(roleRuleRepository.findAll());
        });
        okButton.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancelButton = new Button("Cancel", e -> {
            dialog.close();
        });
        binder.addStatusChangeListener((event) -> {
            okButton.setEnabled(!event.hasValidationErrors());
        });

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(okButton);

        Select<UserMatcherInfo> userMatchers = new Select<>();
        List<UserMatcherInfo> allUserMatchers = userMatcherCollector.getAllUserMatcherInfo();
        userMatchers.setItems(allUserMatchers);
        userMatchers.setEmptySelectionAllowed(false);
        userMatchers.setLabel("Role Rule Type");

        TextField parameter = new TextField();
        parameter.setClearButtonVisible(true);
        LdapAutoComplete parameterComplete = new LdapAutoComplete(
                parameter, ldapService
        );
        parameterComplete.setValueConverter((value) -> value.name());

        //UsersGroupsAutocomplete parameter = new UsersGroupsAutocomplete(ldapSettings, 5);
        parameter.setWidthFull();

        Select<Role> roles = new Select<>();
        roles.setLabel(("Role"));
        Role[] allRoles = Role.values();
        roles.setItems(Role.values());
        roles.setEmptySelectionAllowed(false);

        TextField description = new TextField("Description");
        description.setClearButtonVisible(true);

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
            if (umi.getParameterLabel() == null || umi.getParameterLabel().isEmpty()) {
                parameter.setEnabled(false);
                parameter.setLabel("Without parameter");
            } else {
                parameter.setEnabled(true);
                parameter.setLabel(umi.getParameterLabel());
            }
            String className = umi.getClassName();
            if (className.equals(UsernameMatcher.class.getName())) {
                parameterComplete.setCompleteMode(LdapAutoComplete.CompleteMode.USERS);
            } else if (className.equals(LdapGroupUserMatcher.class.getName())) {
                parameterComplete.setCompleteMode(LdapAutoComplete.CompleteMode.GROUPS);
            } else {
                parameterComplete.setCompleteMode(LdapAutoComplete.CompleteMode.NULL);
            }

            binder.validate();
        });

        userMatchers.setValue(allUserMatchers.get(1));
        binder.validate();

        dialog.add(new FormLayout(
                userMatchers,
                parameter,
                parameterComplete,
                roles,
                description
        ));

        dialog.open();
    }
}
