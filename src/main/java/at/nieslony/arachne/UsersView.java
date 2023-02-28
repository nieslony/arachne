/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.roles.RolesCollector;
import at.nieslony.arachne.users.ArachneUser;
import at.nieslony.arachne.users.UserRepository;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    final Grid<ArachneUser> usersGrid;

    public UsersView(UserRepository userRepository, RolesCollector rolesCollector) {
        this.userRepository = userRepository;
        this.rolesCollector = rolesCollector;

        usersGrid = new Grid<>(ArachneUser.class, false);

        Button addUserButton = new Button("Add User...",
                event -> addUser()
        );

        Grid.Column<ArachneUser> usernameColumn = usersGrid
                .addColumn(ArachneUser::getUsername)
                .setHeader("Username");
        Grid.Column<ArachneUser> displayNameColumn = usersGrid
                .addColumn(ArachneUser::getDisplayName)
                .setHeader("Displayname");
        Grid.Column<ArachneUser> emailColumn = usersGrid
                .addColumn(ArachneUser::getEmail)
                .setHeader("E-Mail");
        usersGrid.addComponentColumn((user) -> {
            String roles = rolesCollector
                    .findRoleDescriptionsForUser(user.getUsername())
                    .stream()
                    .collect(Collectors.joining(", "));
            return new Text(roles);
        }).setHeader("Roles");

        List<ArachneUser> users = userRepository.findAll();
        usersGrid.setItems(users);

        add(addUserButton, usersGrid);
    }

    void addUser() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add User");
        Binder<ArachneUser> binder = new Binder(ArachneUser.class);

        Button okButton = new Button("OK",
                event -> {
                    ArachneUser newUser = new ArachneUser();
                    if (binder.writeBeanIfValid(newUser)) {
                        userRepository.save(newUser);
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

        TextField usernameField = new TextField("Username");
        usernameField.setValue("");
        TextField displayNameField = new TextField("Display Name");
        PasswordField passwordField = new PasswordField("Password");
        PasswordField retypePasswordField = new PasswordField("Retype Password");

        binder.addStatusChangeListener((event) -> {
            okButton.setEnabled(!event.hasValidationErrors());
        });

        binder.forField(usernameField)
                .asRequired()
                .withValidator(username -> {
                    return username.matches("^[a-z].*");
                }, "Username must start with lowercase letter")
                .withValidator(username -> {
                    return username.matches("^[a-z0-9_.\\-]+$");
                }, "Allowed characters: a-z 0-9 - . -")
                .withValidator(username -> {
                    ArachneUser user = userRepository.findByUsername(username);
                    return user == null;
                }, "User already exists")
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

        passwordField.addValueChangeListener((event) -> {
            binder.validate();
        });

        binder.validate();

        dialog.add(new FormLayout(
                usernameField,
                displayNameField,
                passwordField,
                retypePasswordField));

        dialog.open();
    }
}
