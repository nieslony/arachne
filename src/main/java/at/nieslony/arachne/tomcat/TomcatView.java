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
package at.nieslony.arachne.tomcat;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.ShowNotification;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "tomcat", layout = ViewTemplate.class)
@PageTitle("Tomcat Settings")
@RolesAllowed("ADMIN")
public class TomcatView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(TomcatView.class);

    public TomcatView(Settings settings, TomcatService tomcatService) {
        Binder<TomcatSettings> binder = new Binder<>(TomcatSettings.class);
        TomcatSettings tomcatSettings = settings.getSettings(TomcatSettings.class);

        Checkbox enableAjpField = new Checkbox("Enable AJP Connector");
        enableAjpField.setValue(true);

        IntegerField ajpPortField = new IntegerField("Port");
        ajpPortField.setMin(1);
        ajpPortField.setMax(65535);
        ajpPortField.setStepButtonsVisible(true);

        TextField ajpLocationField = new TextField("Outsite Location");
        ajpLocationField.setClearButtonVisible(true);

        Checkbox enableAjpSecretField = new Checkbox("Enable AJP Secret");
        enableAjpField.addClassNames(LumoUtility.FlexWrap.NOWRAP);
        enableAjpSecretField.setValue(true);
        PasswordField ajpSecretField = new PasswordField();
        Button createSecret = new Button(
                "Create new Secret",
                (e) -> {
                    ajpSecretField.setValue(tomcatSettings.createSecret());
                }
        );
        HorizontalLayout secretLayout = new HorizontalLayout();
        secretLayout.add(
                enableAjpSecretField,
                ajpSecretField,
                createSecret
        );
        secretLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        secretLayout.setFlexGrow(1, ajpSecretField);

        Button saveAndRestartButton = new Button(
                "Save",
                e -> {
                    try {
                        tomcatSettings.save(settings);
                        tomcatService.saveApacheConfig();
                        ShowNotification.info(
                                "Tomcat Configuration Saved",
                                """
                                You can also find a configuration for Apache
                                HTTP Server at %s.

                                Please copy or symlink it to your apache
                                configuration folder e.g. /etc/httpt/conf.d and
                                restart apache.
                                """.formatted(tomcatService.getApacheConfigFileName())
                        );
                    } catch (SettingsException ex) {
                        logger.error("Cannot save tomcat settings: " + ex.getMessage());
                    }
                    e.getSource().setEnabled(true);
                });
        saveAndRestartButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

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
                        createSecret.setEnabled(true);
                    } else {
                        ajpPortField.setEnabled(false);
                        enableAjpSecretField.setEnabled(false);
                        ajpSecretField.setEnabled(false);
                        ajpLocationField.setEnabled(false);
                        createSecret.setEnabled(false);
                    }
                });
        enableAjpSecretField.addValueChangeListener(
                e -> {
                    ajpSecretField.setEnabled(e.getValue());
                });

        binder.setBean(tomcatSettings);
        binder.validate();

        FormLayout settingsLayout = new FormLayout(
                ajpPortField,
                ajpLocationField,
                secretLayout
        );
        settingsLayout.setMaxWidth(100, Unit.EX);
        settingsLayout.setColspan(secretLayout, 2);

        add(
                enableAjpField,
                settingsLayout,
                saveAndRestartButton
        );
        setPadding(false);
    }
}
