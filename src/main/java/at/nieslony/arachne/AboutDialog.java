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

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
public class AboutDialog extends Dialog {

    public AboutDialog(ArachneVersion arachneVersion) {
        super("About Arachne");

        String iconPath = "/icons/arachne.svg";
        Image arachneImage = new Image(
                DownloadHandler.forClassResource(getClass(), iconPath),
                "Arachne Logo"
        );
        arachneImage.setWidth(5, Unit.EM);
        arachneImage.setHeight(5, Unit.EM);

        Div arachneInfo = new Div(
                new Paragraph("Arachne version %s"
                        .formatted(arachneVersion.getPrettyVersion())
                ),
                new Paragraph("Copyright ⓒ 2024 by Claas Nieslony")
        );

        if (!arachneVersion.getGitCommitTime().isEmpty()) {
            arachneInfo.add(new Paragraph("Commit time " + arachneVersion.getGitCommitTime()));
        }
        if (!arachneVersion.getGitRemoteOriginUrl().isEmpty()) {
            arachneInfo.add(new Paragraph("Source URL " + arachneVersion.getGitRemoteOriginUrl()));
        }

        add(
                new HorizontalLayout(
                        arachneImage,
                        arachneInfo
                )
        );

        Button closeButton = new Button("Close", click -> {
            close();
        });

        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getFooter()
                .add(closeButton);
    }
}
