/*
 * Copyright (C) 2026 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.ldap.LdapController;
import at.nieslony.arachne.utils.ByteArrayHolder;
import at.nieslony.arachne.utils.components.ShowNotification;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.server.streams.UploadHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.CommunicationException;

@Slf4j
public class EditYourselfDialog extends Dialog {

    private final UserModel user;
    private final LdapController ldapController;
    private final UserRepository userRepository;

    public EditYourselfDialog(
            UserModel user,
            UserRepository userRepository,
            LdapController ldapController
    ) {
        this.user = user;
        this.ldapController = ldapController;
        this.userRepository = userRepository;

        setHeaderTitle(user.getDisplayName() + "'s personal Settings");

        ByteArrayHolder avatarHolder = new ByteArrayHolder(user.getAvatar());
        Binder<UserModel> binder = new Binder<>();

        Select<UserModel.ThemeVariant> themeVariantSelect = new Select<>();
        themeVariantSelect.setLabel("Thema Variant");
        themeVariantSelect.setItems(UserModel.ThemeVariant.values());
        themeVariantSelect.setValue(UserModel.ThemeVariant.Auto);
        binder.forField(themeVariantSelect)
                .bind(UserModel::getThemeVariant, UserModel::setThemeVariant);

        Select<UserModel.AvatarSource> avatarSource = new Select<>();
        avatarSource.setLabel("Avatar Source");
        avatarSource.setItems(UserModel.AvatarSource.values());
        avatarSource.setValue(UserModel.AvatarSource.LDAP);
        binder.forField(avatarSource)
                .bind(UserModel::getAvatarSource, UserModel::setAvatarSource);

        Avatar avatarImg = new Avatar();
        if (user.hasAvatar()) {
            avatarImg.setImageHandler(event -> {
                event.getOutputStream().write(user.getAvatar());
            });
        }
        Upload avatarUpload = new Upload(UploadHandler
                .inMemory((um, bytes) -> {
                    avatarImg.setImageHandler(event -> event.getOutputStream().write(bytes));
                    avatarHolder.set(bytes);
                }));
        avatarUpload.setMaxFiles(1);
        avatarUpload.setMaxFileSize(512 * 1024);
        HorizontalLayout avatarLayout = new HorizontalLayout(
                avatarImg,
                avatarUpload
        );
        avatarLayout.setMargin(false);
        avatarLayout.setPadding(false);
        avatarLayout.setEnabled(false);
        avatarLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        avatarSource.addValueChangeListener((e) -> {
            avatarLayout.setEnabled(e.getValue() == UserModel.AvatarSource.Custom);
            if (e.getValue() == UserModel.AvatarSource.LDAP) {
                try {
                    var ldapUser = ldapController.getUser(user.getUsername());
                    byte[] newAvatar = ldapUser.getAvatar();
                    if (newAvatar != null) {
                        log.debug("Got avatar fot user %s with %d bytes".formatted(user.getUsername(), newAvatar.length));
                    } else {
                        log.debug("No avatar for usr %s found.".formatted(user.getUsername()));
                    }
                    avatarHolder.set(newAvatar);
                    avatarImg.setImageHandler(event -> event.getOutputStream().write(newAvatar));
                } catch (CommunicationException ex) {
                    ShowNotification.error("Cannot load Avatar from LDAP", ex.getMessage());
                }
            }
        });
        Button okButton = new Button("Ok", e -> {
            try {
                binder.writeBean(user);
                user.setAvatar(avatarHolder.get());
                userRepository.save(user);
                UI.getCurrent().getPage().reload();
            } catch (ValidationException ex) {
                log.error("Error validating user: " + ex.toString());
            }
            close();
        });
        okButton.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancelButton = new Button("Cancel", e -> close());

        binder.readBean(user);

        VerticalLayout layout = new VerticalLayout(
                themeVariantSelect,
                avatarSource,
                avatarLayout
        );
        layout.setMargin(false);
        layout.setPadding(false);

        add(layout);

        getFooter().add(
                cancelButton,
                okButton
        );
    }
}
