/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.mail.MailSettings;
import at.nieslony.arachne.mail.MailSettingsRestController;
import at.nieslony.arachne.openvpn.OpenVpnRestController;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.roles.Role;
import at.nieslony.arachne.roles.RoleRuleModel;
import at.nieslony.arachne.roles.RoleRuleRepository;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.usermatcher.UsernameMatcher;
import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

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
    final private RoleRuleRepository roleRuleRepository;
    final private OpenVpnRestController openVpnRestController;
    final private Settings settings;
    final private MailSettingsRestController mailSettingsRestController;

    final Grid<ArachneUser> usersGrid;
    final Grid.Column<ArachneUser> usernameColumn;
    final Grid.Column<ArachneUser> displayNameColumn;
    final Grid.Column<ArachneUser> emailColumn;
    final Grid.Column<ArachneUser> userSourceColumn;

    DataProvider<ArachneUser, Void> userDataProvider;
    final UserSettings userSettings;

    public UsersView(
            UserRepository userRepository,
            RoleRuleRepository roleRuleRepository,
            ArachneUserDetailsService userDetails,
            OpenVpnRestController openVpnRestController,
            Settings settings,
            MailSettingsRestController mailSettingsRestController
    ) {
        this.userRepository = userRepository;
        this.roleRuleRepository = roleRuleRepository;
        this.settings = settings;
        this.userSettings = new UserSettings(settings);
        this.openVpnRestController = openVpnRestController;
        this.mailSettingsRestController = mailSettingsRestController;

        userDataProvider
                = DataProvider.fromCallbacks(
                        query -> {
                            int offset = query.getOffset();
                            int limit = query.getLimit();
                            return userRepository
                                    .findAll()
                                    .stream()
                                    .peek((user) -> {
                                        userDetails.ensureUpdated(
                                                user,
                                                userSettings.getExpirationTimeout()
                                        );
                                    });
                        },
                        query -> (int) userRepository.count()
                );

        usersGrid = new Grid<>(ArachneUser.class, false);
        Button addUserButton = new Button("Add User...",
                event -> addUser()
        );
        addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button userSettingsButton = new Button("Settings...",
                (e) -> openUserSettings()
        );

        HorizontalLayout buttons = new HorizontalLayout(
                addUserButton,
                userSettingsButton
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
        userSourceColumn = usersGrid
                .addColumn(new ComponentRenderer<>((ArachneUser user) -> {
                    String source = user.getExternalProvider();
                    if (source == null) {
                        return new Text("Internal");
                    } else {
                        return new Text(source);
                    }
                }))
                .setHeader("User Source");
        usersGrid.addComponentColumn((user) -> {
            String roles = user.getRolesWithName()
                    .stream()
                    .collect(Collectors.joining(", "));
            return new Text(roles);
        }).setHeader("Roles");

        editUsersGridBuffered();

        usersGrid.setItems(userDataProvider);

        add(buttons, usersGrid);
    }

    private Component getUserEditMenu(ArachneUser user, Editor<ArachneUser> editor) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String myUsername = authentication.getName();

        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
        MenuItem menuItem = menuBar.addItem(new Icon(VaadinIcon.CHEVRON_DOWN));
        SubMenu userMenu = menuItem.getSubMenu();

        if (!user.getUsername().equals(myUsername)) {
            Button editButton = new Button("Edit");
            editButton.addClickListener(e -> {
                if (editor.isOpen()) {
                    editor.cancel();
                }
                editor.editItem(user);
            });
            MenuItem editItem = menuBar.addItem(editButton);

            userMenu.addItem("Change Password...", event -> changePassword(user));
            userMenu.addItem("Delete...", event -> deleteUser(user));
        }
        if (user.getRoles().contains("USER")) {
            OpenVpnUserSettings openVpnUserSettings = new OpenVpnUserSettings(settings);
            FileDownloadWrapper link = new FileDownloadWrapper(
                    openVpnUserSettings.getClientConfigName(),
                    () -> {
                        try {
                            String config = openVpnRestController
                                    .openVpnUserConfig(user.getUsername());
                            return config.getBytes();
                        } catch (PkiException ex) {
                            logger.error(
                                    "Cannot send openvpn config: " + ex.getMessage());
                            return "".getBytes();
                        }
                    }
            );
            link.setText("Download Config");
            userMenu.addItem(link);
            userMenu.addItem("Send openVPN config as E-Mail...", (e) -> {
                sendVpnConfig(user);
            });
        }
        if (!userMenu.getItems().isEmpty()) {
            menuBar.addItem(menuItem);
        }
        if (menuBar.getItems().size() > 1) {
            return menuBar;
        } else {
            return new Text("");
        }
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
                .addComponentColumn((ArachneUser user) -> getUserEditMenu(user, editor))
                .setWidth("15em")
                .setFlexGrow(0);

        TextField usernameField = new TextField();
        usernameField.setValueChangeMode(ValueChangeMode.EAGER);

        usernameField.setWidthFull();

        binder.forField(usernameField)
                .withValidator(usernameValidator)
                .withValidator(usernameUniqueValidator)
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
                .withValidator(usernameValidartor)
                .withValidator(usernameUniqueValidator)
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

    private void openUserSettings() {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("User Settings");

        IntegerField expirationTimeoutField = new IntegerField("Expiration Timeout");
        expirationTimeoutField.setStepButtonsVisible(true);
        Div suffix = new Div();
        suffix.setText("min");
        expirationTimeoutField.setSuffixComponent(suffix);
        expirationTimeoutField.setValue(userSettings.getExpirationTimeout());

        dlg.add(expirationTimeoutField);

        Button okButton = new Button("OK", (e) -> {
            userSettings.setExpirationTimeout(expirationTimeoutField.getValue());
            userSettings.save(settings);
            dlg.close();
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        okButton.setAutofocus(true);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        dlg.getFooter().add(cancelButton, okButton);

        dlg.open();
    }

    void sendVpnConfig(ArachneUser user) {
        MailSettings mailSettings = new MailSettings(settings);

        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Send %s' Config as E-Mail".formatted(user.getDisplayName()));

        EmailField emailField = new EmailField("Destination E-Mail Address");
        emailField.setRequired(true);
        emailField.setErrorMessage("Invalid E-Mail Address");
        if (user.getEmail() != null) {
            emailField.setValue(user.getEmail());
        }
        emailField.setWidthFull();

        dlg.add(emailField);

        Button okButton = new Button("Send", (e) -> {
            try {
                String mailAddr = emailField.getValue();
                String subject = "%s's openVPN settings".formatted(user.getDisplayName());
                mailSettingsRestController.sendConfigMail(
                        mailSettings,
                        user,
                        mailAddr,
                        subject
                );
                Notification.show("Config sent to " + mailAddr);
            } catch (IOException | MessagingException | PkiException ex) {
                String msg = "Error sending e-mail to %s: %s"
                        .formatted(user.getEmail(), ex.getMessage());
                logger.error(msg);
                Notification.show(msg);
            }

            dlg.close();
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        okButton.setAutofocus(true);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        dlg.getFooter().add(cancelButton, okButton);
        dlg.open();
    }
}
