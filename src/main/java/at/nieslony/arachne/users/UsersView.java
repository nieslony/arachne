/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.roles.Role;
import at.nieslony.arachne.roles.RoleRuleModel;
import at.nieslony.arachne.roles.RoleRuleRepository;
import at.nieslony.arachne.roles.RolesCollector;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author claas
 */
@Route(value = "users", layout = ViewTemplate.class)
@PageTitle("Users | Arachne")
@RolesAllowed("ADMIN")
public class UsersView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(UsersView.class);

    final private UserRepository userRepository;
    final private RolesCollector rolesCollector;
    final private RoleRuleRepository roleRuleRepository;

    final Grid<ArachneUser> usersGrid;
    final Grid.Column<ArachneUser> usernameColumn;
    final Grid.Column<ArachneUser> displayNameColumn;
    final Grid.Column<ArachneUser> emailColumn;

    public UsersView(
            UserRepository userRepository,
            RolesCollector rolesCollector,
            RoleRuleRepository roleRuleRepository
    ) {
        this.userRepository = userRepository;
        this.rolesCollector = rolesCollector;
        this.roleRuleRepository = roleRuleRepository;

        usersGrid = new Grid<>(ArachneUser.class, false);

        Button addUserButton = new Button("Add User...",
                event -> addUser()
        );

        usernameColumn = usersGrid
                .addColumn(ArachneUser::getUsername)
                .setHeader("Username");
        displayNameColumn = usersGrid
                .addColumn(ArachneUser::getDisplayName)
                .setHeader("Displayname");
        emailColumn = usersGrid
                .addColumn(ArachneUser::getEmail)
                .setHeader("E-Mail");
        usersGrid.addComponentColumn((user) -> {
            String roles = rolesCollector
                    .findRoleDescriptionsForUser(user.getUsername())
                    .stream()
                    .collect(Collectors.joining(", "));
            return new Text(roles);
        }).setHeader("Roles");

        editUsersGridBuffered();

        List<ArachneUser> users = userRepository.findAll();
        usersGrid.setItems(users);

        add(addUserButton, usersGrid);
    }

    final void editUsersGridBuffered() {
        Editor<ArachneUser> editor = usersGrid.getEditor();
        Binder<ArachneUser> binder = new Binder(ArachneUser.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

        UsernameValidator usernameValidator = new UsernameValidator();
        UsernameUniqueValidator usernameUniqueValidator
                = new UsernameUniqueValidator(userRepository);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String myUsername = authentication.getName();

        Grid.Column<ArachneUser> editColumn = usersGrid
                .addComponentColumn(user -> {
                    if (!user.getUsername().equals(myUsername)) {
                        Button editButton = new Button("Edit");
                        editButton.addClickListener(e -> {
                            if (editor.isOpen()) {
                                editor.cancel();
                            } else {
                                usernameUniqueValidator.setUserId(user.getId());
                            }
                            editor.editItem(user);
                        });
                        MenuBar menuBar = new MenuBar();
                        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
                        MenuItem editItem = menuBar.addItem(editButton);
                        MenuItem menuItem = menuBar.addItem(new Icon(VaadinIcon.CHEVRON_DOWN));
                        SubMenu userMenu = menuItem.getSubMenu();
                        userMenu.addItem(
                                "Change Password...",
                                event -> changePassword(user)
                        );
                        userMenu.addItem(
                                "Delete...",
                                event -> deleteUser(user)
                        );
                        return menuBar;
                    } else {
                        return new Text("");
                    }
                })
                .setWidth("15em")
                .setFlexGrow(0);

        TextField usernameField = new TextField();
        usernameField.setValueChangeMode(ValueChangeMode.EAGER);

        usernameField.setWidthFull();

        binder.forField(usernameField)
                .withValidator(
                        usernameValidator,
                        UsernameValidator.getErrorMsg())
                .withValidator(
                        usernameUniqueValidator,
                        UsernameUniqueValidator.getErrorMsg())
                .bind(ArachneUser::getUsername, ArachneUser::setUsername);
        usernameColumn.setEditorComponent(usernameField);

        TextField displayNameField = new TextField();

        displayNameField.setWidthFull();

        binder.forField(displayNameField)
                .asRequired("Value required")
                .bind(ArachneUser::getDisplayName, ArachneUser::setDisplayName);
        displayNameColumn.setEditorComponent(displayNameField);

        EmailField emailField = new EmailField();

        emailField.setWidthFull();

        binder.forField(emailField)
                .withValidator(new EmailValidator(
                        "This doesn't look like a valid email address",
                        true)
                )
                .bind(ArachneUser::getEmail, ArachneUser::setEmail);
        emailColumn.setEditorComponent(emailField);

        editor.addSaveListener(
                (event) -> {
                    ArachneUser user = event.getItem();
                    userRepository.save(user);
                }
        );

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

        actions.setPadding(
                false);
        editColumn.setEditorComponent(actions);

        binder.addStatusChangeListener(
                (event) -> {
                    saveButton.setEnabled(!event.hasValidationErrors());
                }
        );
    }

    void addUser() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add User");
        Binder<ArachneUser> binder = new Binder(ArachneUser.class
        );

        TextField usernameField = new TextField("Username");
        usernameField.setValue("");
        usernameField.setValueChangeMode(ValueChangeMode.EAGER);
        TextField displayNameField = new TextField("Display Name");
        PasswordField passwordField = new PasswordField("Password");
        passwordField.setValueChangeMode(ValueChangeMode.EAGER);
        PasswordField retypePasswordField = new PasswordField("Retype Password");
        retypePasswordField.setValueChangeMode(ValueChangeMode.EAGER);

        CheckboxGroup<Role> rolesField = new CheckboxGroup<>();
        rolesField.setLabel("Roles");
        rolesField.setItems(Role.ADMIN, Role.USER);

        UsernameValidator usernameValidartor = new UsernameValidator();
        UsernameUniqueValidator usernameUniqueValidator
                = new UsernameUniqueValidator(userRepository);
        binder.forField(usernameField)
                .asRequired()
                .withValidator(
                        usernameValidartor,
                        UsernameValidator.getErrorMsg())
                .withValidator(usernameUniqueValidator,
                        UsernameUniqueValidator.getErrorMsg())
                .bind(ArachneUser::getUsername, ArachneUser::setUsername);
        binder.forField(displayNameField)
                .bind(ArachneUser::getDisplayName, ArachneUser::setDisplayName);
        binder.forField(passwordField)
                .bind(ArachneUser::getPassword, ArachneUser::setPassword);
        AtomicReference<String> retypePasswordStr = new AtomicReference<>("");
        binder.forField(retypePasswordField)
                .withValidator(
                        retypePassword -> {
                            String password = passwordField.getValue();
                            return password.equals(retypePassword);
                        },
                        "Passwords don't match")
                .bind((source) -> retypePasswordStr.get(), (bean, fieldvalue) -> {
                    retypePasswordStr.set(fieldvalue);
                });

        Button okButton = new Button("OK",
                event -> {
                    ArachneUser newUser = new ArachneUser();
                    if (binder.writeBeanIfValid(newUser)) {
                        userRepository.save(newUser);

                        for (Role role : rolesField.getValue()) {
                            RoleRuleModel rrm = new RoleRuleModel(
                                    UsernameMatcher.class,
                                    newUser.getUsername(),
                                    role
                            );
                            roleRuleRepository.save(rrm);
                        }

                        dialog.close();

                        usersGrid.setItems(userRepository.findAll());
                    }
                });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelButton = new Button("Cancel",
                event -> {
                    dialog.close();
                });
        dialog.getFooter().add(cancelButton, okButton);

        passwordField.addValueChangeListener((event) -> {
            binder.validate();
        });

        binder.addStatusChangeListener((event) -> {
            okButton.setEnabled(!event.hasValidationErrors());
        });

        binder.validate();

        dialog.add(new FormLayout(
                usernameField,
                displayNameField,
                passwordField,
                retypePasswordField,
                rolesField));

        dialog.open();
    }

    void changePassword(ArachneUser user) {
        ChangePasswordDialog dlg = new ChangePasswordDialog(
                userRepository,
                user
        );
        dlg.open();
    }

    void deleteUser(ArachneUser user) {
        ConfirmDialog confirm = new ConfirmDialog();
        String username = user.getUsername();
        confirm.setHeader("Delete user \"%s\"".formatted(username));
        confirm.setText(
                "Are you sure you want to permanently delete user \"%s\"?"
                        .formatted(username)
        );
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(
                e -> {
                    userRepository.delete(user);
                    usersGrid.setItems(userRepository.findAll());
                });

        confirm.open();
    }
}
