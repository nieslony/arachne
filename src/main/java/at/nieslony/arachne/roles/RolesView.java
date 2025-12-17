/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.roles;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.usermatcher.LdapGroupUserMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.usermatcher.UserMatcherInfo;
import at.nieslony.arachne.usermatcher.UsernameMatcher;
import at.nieslony.arachne.utils.components.LdapAutoComplete;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
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
    final private LdapSettings ldapSettings;

    final Grid<RoleRuleModel> roleRules;
    Grid.Column<RoleRuleModel> ruleColumn;
    Grid.Column<RoleRuleModel> parameterColumn;
    Grid.Column<RoleRuleModel> roleColumn;
    Grid.Column<RoleRuleModel> descriptionColumn;

    public RolesView(
            RoleRuleRepository roleRuleRepository,
            UserMatcherCollector userMatcherCollector,
            Settings settings
    ) {
        this.roleRuleRepository = roleRuleRepository;
        this.userMatcherCollector = userMatcherCollector;
        this.ldapSettings = settings.getSettings(LdapSettings.class);

        Button addRole = new Button("Add...", e -> {
            addRule();
        });

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

        roleRules.setItems(roleRuleRepository.findAll());

        editRoleBuffered();

        add(topButtons, roleRules);
        setPadding(false);
    }

    private void editRoleBuffered() {
        Editor<RoleRuleModel> editor = roleRules.getEditor();
        Binder<RoleRuleModel> binder = new Binder<>(RoleRuleModel.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

        Grid.Column<RoleRuleModel> editColumn = roleRules
                .addComponentColumn(roleRule -> {
                    MenuBar menuBar = new MenuBar();
                    menuBar.addThemeVariants(
                            MenuBarVariant.LUMO_SMALL,
                            MenuBarVariant.LUMO_ICON
                    );

                    menuBar.addItem("Edit", e -> {
                        if (editor.isOpen()) {
                            editor.cancel();
                        }
                        editor.editItem(roleRule);
                    });

                    MenuItem moreItem = menuBar.addItem(new Icon(VaadinIcon.CHEVRON_DOWN));
                    SubMenu moreMenu = moreItem.getSubMenu();
                    moreMenu.addItem("Delete...", e -> {
                        Div ruleTxt = new Div(roleRule.toString());
                        ruleTxt.addClassName(LumoUtility.FontWeight.BOLD);
                        ruleTxt.setWhiteSpace(HasText.WhiteSpace.NOWRAP);
                        Div msg = new Div(
                                new Text("Really remove role rule "),
                                ruleTxt,
                                new Text("now? This can't be undone.")
                        );
                        ConfirmDialog confirm = new ConfirmDialog();
                        confirm.setHeader("Remove rule");
                        confirm.setText(msg);
                        confirm.setConfirmButton("Delete",
                                ce -> {
                                    roleRuleRepository.delete(roleRule);
                                    roleRules.setItems(roleRuleRepository.findAll());
                                }
                        );
                        confirm.setCancelable(true);
                        confirm.setWidth(32, Unit.EM);
                        confirm.open();
                    });

                    return menuBar;
                })
                .setWidth("10em")
                .setFlexGrow(0);

        Select<UserMatcherInfo> userMatchersField = new Select<>();
        List<UserMatcherInfo> allUserMatchers = userMatcherCollector.getAllUserMatcherInfo();
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
        LdapAutoComplete parameterFieldComplete = new LdapAutoComplete(
                parameterField,
                ldapSettings
        );
        parameterColumn.setEditorComponent(new HorizontalLayout(
                parameterField,
                parameterFieldComplete
        ));

        Select<Role> roles = new Select<>();
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
        okButton.addThemeVariants(ButtonVariant.AURA_PRIMARY);
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
        userMatchers.setLabel("Role Rules");

        TextField parameter = new TextField();
        parameter.setClearButtonVisible(true);
        LdapAutoComplete parameterComplete = new LdapAutoComplete(parameter, ldapSettings);
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
