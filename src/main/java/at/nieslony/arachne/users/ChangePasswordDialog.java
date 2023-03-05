package at.nieslony.arachne.users;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/*
 * Copyright (C) 2023 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 *
 * @author claas
 */
@UIScope
@Component
public class ChangePasswordDialog extends Dialog {

    private static final Logger logger = LoggerFactory.getLogger(ChangePasswordDialog.class);

    private UserRepository userRepository;
    private ArachneUser forUser = null;

    @Getter
    @Setter
    class PasswordChanger {

        private String currentPassword;
        private String newPassword;
        private String retypeNewPassword;
    }

    public ChangePasswordDialog(UserRepository userRepository, ArachneUser forUser) {
        this.forUser = forUser;
        this.userRepository = userRepository;

        createDialog();
    }

    public ChangePasswordDialog(UserRepository userRepository) {
        this.userRepository = userRepository;

        createDialog();
    }

    void createDialog() {
        setHeaderTitle("Change Password");
        Binder<PasswordChanger> binder = new Binder(PasswordChanger.class);

        PasswordField currentPasswordField = null;
        ArachneUser user;
        if (forUser == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            user = userRepository.findByUsername(authentication.getName());
            String currentPassword = user.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

            currentPasswordField = new PasswordField("Current Password");
            currentPasswordField.setWidth(24, Unit.EM);
            binder.forField(currentPasswordField)
                    .withValidator(
                            password -> passwordEncoder
                                    .matches(
                                            password,
                                            currentPassword),
                            "Wrong password")
                    .bind(PasswordChanger::getCurrentPassword, PasswordChanger::setCurrentPassword);
        } else {
            user = this.forUser;
        }

        PasswordField newPassword = new PasswordField("New Password");
        newPassword.setWidth(24, Unit.EM);
        binder.forField(newPassword)
                .bind(PasswordChanger::getNewPassword, PasswordChanger::setNewPassword);

        PasswordField retypeNewPassword = new PasswordField("Retype new Password");
        retypeNewPassword.setWidth(24, Unit.EM);
        binder.forField(retypeNewPassword)
                .withValidator(
                        e -> newPassword
                                .getValue()
                                .equals(retypeNewPassword.getValue()),
                        "Password don't match"
                )
                .bind(PasswordChanger::getRetypeNewPassword, PasswordChanger::setRetypeNewPassword);

        Button okButton = new Button("OK", e -> {
            user.setPassword(newPassword.getValue());
            userRepository.save(user);
            close();

        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelButton = new Button("Cancel", e -> close());

        getFooter().add(cancelButton, okButton);

        binder.addStatusChangeListener((event) -> {
            okButton.setEnabled(!event.hasValidationErrors());
        });

        newPassword.addValueChangeListener((event) -> {
            binder.validate();
        });

        if (currentPasswordField == null) {
            add(new VerticalLayout(
                    newPassword,
                    retypeNewPassword
            ));
        } else {
            add(new VerticalLayout(
                    currentPasswordField,
                    newPassword,
                    retypeNewPassword
            ));
        }
    }
}
