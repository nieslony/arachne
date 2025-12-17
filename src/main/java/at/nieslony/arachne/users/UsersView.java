/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.mail.MailSettings;
import at.nieslony.arachne.mail.MailSettingsRestController;
import at.nieslony.arachne.openvpn.OpenVpnController;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.roles.Role;
import at.nieslony.arachne.roles.RoleRuleModel;
import at.nieslony.arachne.roles.RoleRuleRepository;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.usermatcher.UsernameMatcher;
import at.nieslony.arachne.utils.components.ShowNotification;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
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
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
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
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.mail.MessagingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailSendException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.ClipboardHelper;

/**
 *
 * @author claas
 */
@Route(value = "users", layout = ViewTemplate.class)
@PageTitle("Users")
@RolesAllowed("ADMIN")
@Slf4j
public class UsersView extends VerticalLayout {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRuleRepository roleRuleRepository;

    @Autowired
    private OpenVpnController openVpnRestController;

    @Autowired
    private Settings settings;

    @Autowired
    private MailSettingsRestController mailSettingsRestController;

    Grid<UserModel> usersGrid;
    Grid.Column<UserModel> usernameColumn;
    Grid.Column<UserModel> displayNameColumn;
    Grid.Column<UserModel> emailColumn;
    Grid.Column<UserModel> userSourceColumn;

    DataProvider<UserModel, Void> userDataProvider;
    UserSettings userSettings;

    public UsersView() {
    }

    @PostConstruct
    public void init() {
        this.userSettings = settings.getSettings(UserSettings.class);

        userDataProvider
                = DataProvider.fromCallbacks(
                        query -> {
                            Pageable pageable = PageRequest.of(
                                    query.getOffset(),
                                    query.getLimit()
                            );
                            var page = userRepository.findAll(pageable);
                            return page
                                    .stream()
                                    .peek((user) -> {
                                        // update user !!!
                                        //return user;
                                    });
                        },
                        query -> (int) userRepository.count()
                );

        usersGrid = new Grid<>(UserModel.class, false);
        Button addUserButton = new Button("Add User...",
                event -> addUser()
        );
        addUserButton.addThemeVariants(ButtonVariant.AURA_PRIMARY);

        Button userSettingsButton = new Button("Settings...",
                (e) -> openUserSettings()
        );

        HorizontalLayout buttons = new HorizontalLayout(
                addUserButton,
                userSettingsButton
        );

        usernameColumn = usersGrid
                .addColumn(UserModel::getUsername)
                .setHeader("Username");
        displayNameColumn = usersGrid
                .addColumn(UserModel::getDisplayName)
                .setHeader("Displayname");
        emailColumn = usersGrid
                .addColumn(UserModel::getEmail)
                .setHeader("E-Mail");
        userSourceColumn = usersGrid
                .addColumn(new ComponentRenderer<>((UserModel user) -> {
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
        setPadding(false);
    }

    private Component getUserEditMenu(UserModel user, Editor<UserModel> editor) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String myUsername = authentication.getName();

        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.AURA_TERTIARY);
        MenuItem menuItem = menuBar.addItem(new Icon(VaadinIcon.CHEVRON_DOWN));
        SubMenu userMenu = menuItem.getSubMenu();

        if (user.getExternalProvider() == null) {
            userMenu.addItem("Edit", e -> {
                if (editor.isOpen()) {
                    editor.cancel();
                }
                editor.editItem(user);
            });

            if (!user.getUsername().equals(myUsername)) {
                userMenu.addItem("Change Password...", event -> changePassword(user));
                userMenu.addItem("Delete...", event -> deleteUser(user));
            }
        }

        if (user.getRoles().contains("USER")) {
            OpenVpnUserSettings openVpnUserSettings
                    = settings.getSettings(OpenVpnUserSettings.class);

            var dlh = DownloadHandler.fromInputStream((de) -> {
                try {
                    String config = openVpnRestController
                            .openVpnUserConfig(user.getUsername());
                    var is = new ByteArrayInputStream(config.getBytes());
                    return new DownloadResponse(
                            is,
                            openVpnUserSettings.getClientConfigName(),
                            "application/x-openvpn-profile",
                            config.getBytes().length
                    );
                } catch (PkiException | SettingsException e) {
                    return DownloadResponse.error(500);
                }
            });
            userMenu.addItem(new Anchor(dlh, "Download Config"));
            userMenu.addItem("Send Config as E-Mail…", (e) -> sendVpnConfig(user));
            userMenu.addItem("View Config…", (e) -> viewConfig(user));
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

    private void viewConfig(UserModel user) {
        String configShell;
        String configOvpn;
        try {
            configShell = openVpnRestController.openVpnUserConfigShell(user.getUsername());
            configOvpn = openVpnRestController.openVpnUserConfig(user.getUsername());
        } catch (PkiException | SettingsException ex) {
            log.error("Cannot create usre confifuration: " + ex.getMessage());
            ShowNotification.error("Error", "Cannot create c onfiguration");
            return;
        }

        Dialog dlg = new Dialog("View %s's Configuration".formatted(user.getDisplayName()));
        TabSheet tabSheet = new TabSheet();

        var configShellCopy = new ClipboardHelper(
                configShell,
                new Button(
                        "Copy to clipboard",
                        VaadinIcon.COPY.create()
                )
        );

        Scroller configShellScroller = new Scroller(new Pre(configShell));
        configShellScroller.setWidth(41, Unit.EM);
        configShellScroller.setHeight(48, Unit.EX);

        var configShellLayout = new VerticalLayout(
                configShellCopy,
                configShellScroller
        );
        configShellLayout.setMargin(false);
        configShellLayout.setPadding(false);

        var configOvpnCopy = new ClipboardHelper(
                configOvpn,
                new Button(
                        "Copy to clipboard",
                        VaadinIcon.COPY.create()
                )
        );

        Scroller configOvpnScroller = new Scroller(new Pre(configOvpn));
        configOvpnScroller.setWidth(41, Unit.EM);
        configOvpnScroller.setHeight(48, Unit.EX);

        var configOvpnLayout = new VerticalLayout(
                configOvpnCopy,
                configOvpnScroller
        );
        configShellLayout.setMargin(false);
        configShellLayout.setPadding(false);

        tabSheet.add("Shell Script", configShellLayout);
        tabSheet.add(".ovpn File", configOvpnLayout);
        dlg.add(tabSheet);

        Button closeButton = new Button("Close", e -> dlg.close());
        closeButton.addThemeVariants(ButtonVariant.AURA_PRIMARY);
        dlg.getFooter().add(closeButton);

        dlg.setResizable(true);
        dlg.open();
    }

    final void editUsersGridBuffered() {
        Editor<UserModel> editor = usersGrid.getEditor();
        Binder<UserModel> binder = new Binder<>(UserModel.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

        UsernameValidator usernameValidator = new UsernameValidator();
        UsernameUniqueValidator usernameUniqueValidator
                = new UsernameUniqueValidator(userRepository);

        Grid.Column<UserModel> editColumn = usersGrid
                .addComponentColumn((UserModel user) -> getUserEditMenu(user, editor))
                .setWidth("15em")
                .setFlexGrow(0);

        TextField usernameField = new TextField();
        usernameField.setValueChangeMode(ValueChangeMode.EAGER);

        usernameField.setWidthFull();

        binder.forField(usernameField)
                .withValidator(usernameValidator)
                .withValidator(usernameUniqueValidator)
                .bind(UserModel::getUsername, UserModel::setUsername);
        usernameColumn.setEditorComponent(usernameField);

        TextField displayNameField = new TextField();

        displayNameField.setWidthFull();

        binder.forField(displayNameField)
                .asRequired("Value required")
                .bind(UserModel::getDisplayName, UserModel::setDisplayName);
        displayNameColumn.setEditorComponent(displayNameField);

        EmailField emailField = new EmailField();

        emailField.setWidthFull();

        binder.forField(emailField)
                .withValidator(new EmailValidator(
                        "This doesn't look like a valid email address",
                        true)
                )
                .bind(UserModel::getEmail, UserModel::setEmail);
        emailColumn.setEditorComponent(emailField);

        editor.addSaveListener(
                (event) -> {
                    UserModel user = event.getItem();
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

        actions.setPadding(false);
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
        Binder<UserModel> binder = new Binder<>(UserModel.class
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
                .bind(UserModel::getUsername, UserModel::setUsername);
        binder.forField(displayNameField)
                .bind(UserModel::getDisplayName, UserModel::setDisplayName);
        binder.forField(passwordField)
                .bind(UserModel::getPassword, UserModel::setPassword);
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
                    UserModel newUser = new UserModel();
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
        okButton.addThemeVariants(ButtonVariant.AURA_PRIMARY);
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

    void changePassword(UserModel user) {
        ChangePasswordDialog dlg = new ChangePasswordDialog(
                userRepository,
                user
        );
        dlg.open();
    }

    void deleteUser(UserModel user) {
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
            try {
                userSettings.save(settings);
            } catch (SettingsException ex) {
                log.error("Cannot save user settings: " + ex.getMessage());
            }
            dlg.close();
        });
        okButton.addThemeVariants(ButtonVariant.AURA_PRIMARY);
        okButton.setAutofocus(true);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        dlg.getFooter().add(cancelButton, okButton);

        dlg.open();
    }

    void sendVpnConfig(UserModel user) {
        MailSettings mailSettings = settings.getSettings(MailSettings.class);

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
                ShowNotification.info("Config sent to " + mailAddr);
            } catch (IOException | MailSendException | MessagingException | PkiException | SettingsException ex) {
                String header = "Error sending e-mail to %s".formatted(user.getEmail());
                log.error(header + ": " + ex.getMessage());
                ShowNotification.error(header, ex.getMessage());
            }

            dlg.close();
        });
        okButton.addThemeVariants(ButtonVariant.AURA_PRIMARY);
        okButton.setAutofocus(true);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        dlg.getFooter().add(cancelButton, okButton);
        dlg.open();
    }
}
