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

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.EmailField;
import java.util.function.Consumer;

/**
 *
 * @author claas
 */
public class EnterEMailAddressDialog extends Dialog {

    private final EmailField emailField;

    public EnterEMailAddressDialog(String header, Consumer<String> onOk) {
        setHeaderTitle(header);

        emailField = new EmailField("Destination E-Mail Address");
        emailField.setRequired(true);
        emailField.setErrorMessage("Invalid E-Mail Address");
        emailField.setWidthFull();

        add(emailField);
        setMinWidth(24, Unit.REM);

        Button okButton = new Button("Send", (e) -> {
            close();
            onOk.accept(emailField.getValue());
        });
        okButton.addThemeVariants(ButtonVariant.PRIMARY);
        okButton.setAutofocus(true);

        Button cancelButton = new Button("Cancel", (e) -> {
            close();
        });

        getFooter().add(cancelButton, okButton);
    }

    public void setEMail(String email) {
        emailField.setValue(email);
    }
}
