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
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author claas
 */
@Slf4j
public class EditYourselfDialog extends Dialog {

    @Autowired
    UserRepository userRepository;

    public EditYourselfDialog(UserModel user) {
        setHeaderTitle(user.getDisplayName() + "'s GUI Settings");

        Binder<UserModel> binder = new Binder<>();
        binder.readBean(user);

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

        Avatar avatar = new Avatar();
        if (user.hasAvatar()) {
            avatar.setImageHandler(event -> {
                event.getOutputStream().write(user.getAvatar());
            });
        }
        Upload avatarUpload = new Upload(UploadHandler
                .inMemory((um, bytes) -> {
                    avatar.setImageHandler(event -> event.getOutputStream().write(bytes));
                }));
        avatarUpload.setMaxFiles(1);
        avatarUpload.setMaxFileSize(512 * 1024);
        HorizontalLayout avatarLayout = new HorizontalLayout(
                avatar,
                avatarUpload
        );
        avatarLayout.setMargin(false);
        avatarLayout.setPadding(false);
        avatarLayout.setEnabled(false);
        avatarLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout layout = new VerticalLayout(
                themeVariantSelect,
                avatarSource,
                avatarLayout
        );
        add(layout);

        avatarSource.addValueChangeListener((e) -> {
            avatarLayout.setEnabled(e.getValue() == UserModel.AvatarSource.Custom);
        });
        Button okButton = new Button("OK", e -> {
            try {
                binder.writeBean(user);
                userRepository.save(user);
            } catch (ValidationException ex) {
                log.error("Error validating user: " + ex.toString());
            }
            close();
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelButton = new Button("Cancel", e -> close());

        getFooter().add(cancelButton, okButton);
    }
}
