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
package at.nieslony.arachne;

import at.nieslony.arachne.settings.Settings;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author claas
 */
@Route(value = "tomcat", layout = ViewTemplate.class)
@PageTitle("Tomcat | Arachne")
@RolesAllowed("ADMIN")
public class TomcatView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(TomcatView.class);

    private final Settings settings;

    @Value("${arachneConfigDir}")
    private String arachneConfigDir;

    public TomcatView(Settings settings) {
        this.settings = settings;
        Binder<TomcatSettings> binder = new Binder(TomcatSettings.class);

        Checkbox enableAjpField = new Checkbox("Enable AJP Connector");
        enableAjpField.setValue(true);

        IntegerField ajpPortField = new IntegerField("Port");

        Checkbox enableAjpSecretField = new Checkbox("Enable AJP Secret");
        enableAjpSecretField.setValue(true);
        PasswordField ajpSecretField = new PasswordField();
        HorizontalLayout secretLayout = new HorizontalLayout();
        secretLayout.add(enableAjpSecretField, ajpSecretField);
        secretLayout.setAlignItems(Alignment.CENTER);

        TextField ajpLocationField = new TextField("Outsite Location");

        Button saveAndRestartButton = new Button(
                "Save and Restart Arachne",
                e -> {
                    TomcatSettings tomcatSettings = binder.getBean();
                    tomcatSettings.save(settings);
                    saveApacheConfig(tomcatSettings);
                });

        binder.forField(enableAjpField)
                .bind(TomcatSettings::isEnableAjpConnector, TomcatSettings::setEnableAjpConnector);
        binder.forField(ajpPortField)
                .asRequired()
                .bind(TomcatSettings::getAjpPort, TomcatSettings::setAjpPort);
        binder.forField(enableAjpSecretField)
                .bind(TomcatSettings::isEnableAjpSecret, TomcatSettings::setEnableAjpSecret);
        binder.forField(ajpSecretField)
                .asRequired()
                .bind(TomcatSettings::getAjpSecret, TomcatSettings::setAjpSecret);
        binder.forField(ajpLocationField)
                .bind(TomcatSettings::getAjpLocation, TomcatSettings::setAjpLocation);

        enableAjpField.addValueChangeListener(
                e -> {
                    if (e.getValue()) {
                        ajpPortField.setEnabled(true);
                        enableAjpSecretField.setEnabled(true);
                        ajpSecretField.setEnabled(enableAjpSecretField.getValue());
                        ajpLocationField.setEnabled(true);
                    } else {
                        ajpPortField.setEnabled(false);
                        enableAjpSecretField.setEnabled(false);
                        ajpSecretField.setEnabled(false);
                        ajpLocationField.setEnabled(false);
                    }
                });
        enableAjpSecretField.addValueChangeListener(
                e -> {
                    ajpSecretField.setEnabled(e.getValue());
                });

        TomcatSettings tomcatSettings = new TomcatSettings(settings);
        binder.setBean(tomcatSettings);
        binder.validate();

        add(
                enableAjpField,
                ajpPortField,
                secretLayout,
                ajpLocationField,
                saveAndRestartButton
        );
    }

    private void saveApacheConfig(TomcatSettings tomcatSettings) {
        tomcatSettings.save(settings);

        String fileName = arachneConfigDir + "/arachne.conf";
        String now = new Date().toString();
        StringWriter config = new StringWriter();
        config.write("# Generated by Arachne on %s\n\n".formatted(now));
        config.write("ProxyPass");
        config.write(" \"%s\" ".formatted(tomcatSettings.getAjpLocation()));
        config.write(" \"ajp://127.0.0.1:%d%s\""
                .formatted(
                        tomcatSettings.getAjpPort(),
                        tomcatSettings.getAjpLocation()));
        if (tomcatSettings.isEnableAjpSecret()) {
            config.write(" secret=" + tomcatSettings.getAjpSecret());
        }
        config.write("\n");

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(config.toString());
        } catch (IOException ex) {
            logger.error("Cannot write %s: %s".formatted(fileName, ex.getMessage()));
        }

        Arachne.restart();
    }
}
