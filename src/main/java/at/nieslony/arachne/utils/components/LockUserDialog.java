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
package at.nieslony.arachne.utils.components;

import at.nieslony.arachne.openvpn.management.ManagementException;
import at.nieslony.arachne.openvpn.management.OpenVpnManagementService;
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.UserRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
@Slf4j
public class LockUserDialog extends Dialog {

    private final Checkbox lockUntilFurthoreNotice;
    private final DateTimePicker lockUntil;
    private final Checkbox revoceCertificate;

    private final UserModel user;
    private final BeanFactory beanFactory;

    public LockUserDialog(
            UserModel user,
            BeanFactory beanFactory
    ) {
        this.user = user;
        this.beanFactory = beanFactory;

        VerticalLayout layout = new VerticalLayout();

        lockUntilFurthoreNotice = new Checkbox("Lock until further notice");

        lockUntil = new DateTimePicker("Lock until");
        lockUntil.setStep(Duration.ofMinutes(1));
        lockUntil.setValue(LocalDateTime.ofInstant(
                Instant.now().plus(10, ChronoUnit.MINUTES),
                ZoneId.systemDefault())
        );

        revoceCertificate = new Checkbox("Revoice certitficate");

        layout.add(
                lockUntilFurthoreNotice,
                lockUntil,
                revoceCertificate
        );

        lockUntilFurthoreNotice.addValueChangeListener(
                e -> lockUntil.setEnabled(!e.getValue())
        );

        add(layout);

        Button cancel = new Button("Cancel", e -> close());
        Button ok = new Button("Ok", e -> onOk());

        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getFooter().add(cancel, ok);
    }

    private void onOk() {
        UserRepository userRepository = beanFactory.getBean(UserRepository.class);
        OpenVpnManagementService openVpnManagementService = beanFactory.getBean(OpenVpnManagementService.class);

        user.setLockedUntil(
                lockUntilFurthoreNotice.getValue()
                ? Long.MAX_VALUE
                : lockUntil
                        .getValue()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );

        try {
            userRepository.save(user);
            openVpnManagementService.getUserManagement().killUser(user.getUsername());
        } catch (ManagementException ex) {
            log.error("Error dropping user %s: %s".formatted(user.getUsername(), ex.getMessage()));
        }
        close();
    }
}
