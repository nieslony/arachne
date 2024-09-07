/*
 * Copyright (C) 2024 claas
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
package at.nieslony.arachne;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;

/**
 *
 * @author claas
 */
public class AboutDialog extends Dialog {

    public AboutDialog() {
        super("About Arachne");

        add(
                new Paragraph("Arachne version %s"
                        .formatted(ArachneVersion.ARACHNE_VERSION)
                ),
                new Paragraph("Copyright ⓒ 2024 by Claas Nieslony")
        );

        Button closeButton = new Button("Close", click -> {
            close();
        });
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getFooter().add(closeButton);
    }
}
