/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.roles.RolesCollector;
import at.nieslony.arachne.users.ArachneUser;
import at.nieslony.arachne.users.UserRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
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
@Route(value = "users", layout = ViewTemplate.class)
@PageTitle("Users | Arachne")
@RolesAllowed("ADMIN")
public class UsersView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(UsersView.class);

    final private UserRepository userRepository;
    final private RolesCollector rolesCollector;

    public UsersView(UserRepository userRepository, RolesCollector rolesCollector) {
        this.userRepository = userRepository;
        this.rolesCollector = rolesCollector;

        Grid<ArachneUser> grid = new Grid<>(ArachneUser.class, false);
        Editor<ArachneUser> editor = grid.getEditor();
        Label usernameStatus = new Label();
        Label displayNameStatus = new Label();
        Label emailStatus = new Label();

        Grid.Column<ArachneUser> usernameColumn = grid
                .addColumn(ArachneUser::getUsername)
                .setHeader("Username");
        Grid.Column<ArachneUser> displayNameColumn = grid
                .addColumn(ArachneUser::getDisplayName)
                .setHeader("Displayname");
        Grid.Column<ArachneUser> emailColumn = grid
                .addColumn(ArachneUser::getEmail)
                .setHeader("E-Mail");
        /*grid.addComponentColumn((user) -> {
            String roles = rolesCollector
                    .findRoleDescriptionsForUser(user.getUsername())
                    .stream()
                    .collect(Collectors.joining(", "));
            return new Text(roles);
        }).setHeader("Roles");*/
        Grid.Column<ArachneUser> editColumn = grid
                .addComponentColumn((user) -> {
                    Button editButton = new Button("Edit");
                    editButton.addClickListener((t) -> {
                        if (editor.isOpen()) {
                            editor.cancel();
                        }
                        grid.getEditor().editItem(user);
                    });
                    return editButton;
                });
        Binder<ArachneUser> binder = new Binder<>(ArachneUser.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

        TextField usernameField = new TextField();
        usernameField.setWidthFull();
        binder.forField(usernameField)
                .asRequired("Username is required")
                .withStatusLabel(usernameStatus)
                .bind(ArachneUser::getUsername, ArachneUser::setUsername);
        usernameColumn.setEditorComponent(usernameField);

        TextField displayNameField = new TextField();
        displayNameField.setWidthFull();
        binder.forField(displayNameField)
                .withStatusLabel(displayNameStatus)
                .bind("displayName");
        displayNameColumn.setEditorComponent(displayNameField);

        EmailField emailField = new EmailField();
        emailField.setWidthFull();
        binder.forField(emailField)
                .withValidator(new EmailValidator(
                        "Please enter valid email address",
                        true))
                .withStatusLabel(emailStatus)
                .bind(ArachneUser::getEmail, ArachneUser::setEmail);
        emailColumn.setEditorComponent(emailField);

        Button saveButton = new Button("Save",
                e -> {
                    if (editor.save()) {
                        logger.info("User saved");
                        grid.getEditor().closeEditor();
                    } else {
                        logger.error("Error saving user");
                    }
                });
        Button cancelButton = new Button(VaadinIcon.CLOSE.create(),
                e -> {
                    editor.cancel();
                });
        cancelButton.addThemeVariants(
                ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_ERROR);
        HorizontalLayout actions = new HorizontalLayout(
                saveButton,
                cancelButton
        );
        actions.setPadding(false);
        editColumn.setEditorComponent(actions);

        editor.addCancelListener(e -> {
            usernameStatus.setText("");
            displayNameStatus.setText("");
            emailStatus.setText("");
        });

        List<ArachneUser> users = userRepository.findAll();
        grid.setItems(users);

        add(grid,
                usernameStatus,
                displayNameStatus,
                emailStatus);
    }
}
