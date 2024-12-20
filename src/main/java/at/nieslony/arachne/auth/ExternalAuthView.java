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
package at.nieslony.arachne.auth;

import at.nieslony.arachne.Arachne;
import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.kerberos.KerberosSettings;
import at.nieslony.arachne.kerberos.KeytabException;
import at.nieslony.arachne.kerberos.KeytabFile;
import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.tomcat.TomcatService;
import at.nieslony.arachne.utils.components.ShowNotification;
import at.nieslony.arachne.utils.validators.IgnoringInvisibleOrDisabledValidator;
import at.nieslony.arachne.utils.validators.ServicePrincipalValidator;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "kerberos", layout = ViewTemplate.class)
@PageTitle("External Authentication")
@RolesAllowed("ADMIN")
public class ExternalAuthView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(ExternalAuthView.class);

    private final Settings settings;
    private final TomcatService tomcatService;

    private final Button saveButton;

    private Checkbox enableKerberosAuthField;
    private TextField keytabPathField;
    private ComboBox<String> servicePrincipalField;
    private Button readKeytabButton;
    private Binder<KerberosSettings> kerberosBinder;

    private Checkbox preAuthEnabled;
    private RadioButtonGroup<PreAuthSettings.PreAuthSource> preAuthSource;
    private TextField preAuthEnvVar;
    private TextField preAuthHttpHeader;
    private Checkbox createApacheConfig;
    private TextField apacheKeytabFile;

    private Binder<PreAuthSettings> preAuthBinder;

    public ExternalAuthView(
            Settings settings,
            TomcatService tomcatService
    ) {
        this.settings = settings;
        this.tomcatService = tomcatService;

        LdapSettings ldapSettings = settings.getSettings(LdapSettings.class);
        if (!ldapSettings.isEnableLdapUserSource()) {

            Div warning = new Div();
            warning.addClassNames(
                    LumoUtility.Background.WARNING_10,
                    LumoUtility.TextColor.WARNING,
                    LumoUtility.Padding.MEDIUM,
                    LumoUtility.BorderColor.WARNING,
                    LumoUtility.Border.ALL
            );
            Icon icon = VaadinIcon.WARNING.create();
            Span warningLbl = new Span("Warning:");
            warningLbl.addClassNames(
                    LumoUtility.FontWeight.BOLD,
                    LumoUtility.Padding.SMALL
            );
            Text warningTxt = new Text(
                    """
                    LDAP user source not enabled.
                    External authentification will fail!
                    """
            );
            warning.add(icon, warningLbl, warningTxt);
            warning.addClassNames(LumoUtility.TextColor.WARNING);
            add(warning);
        }

        saveButton = new Button(
                "Save and Restart Arachne",
                e -> onSaveAndRestart()
        );
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setDisableOnClick(true);

        TabSheet tabs = new TabSheet();
        tabs.add("Kerberos", createKerberosView(settings));
        tabs.add("Pre Authentication", createPreAuthView(settings));
        tabs.setWidthFull();

        add(tabs, saveButton);
        setPadding(false);
    }

    private Component createKerberosView(Settings settings) {
        KerberosSettings kerberosSettings = settings.getSettings(KerberosSettings.class);
        kerberosBinder = new Binder<>();
        kerberosBinder.setBean(kerberosSettings);

        enableKerberosAuthField = new Checkbox("Enable Kerberos Authentication");
        kerberosBinder.forField(enableKerberosAuthField)
                .bind(KerberosSettings::isEnableKrbAuth, KerberosSettings::setEnableKrbAuth);

        keytabPathField = new TextField("Keytab Path");
        keytabPathField.setWidthFull();
        kerberosBinder.forField(keytabPathField)
                .withValidator((filename) -> {
                    File f = new File(filename);
                    return f.isFile() && f.canRead();
                }, "Cannot read file")
                .bind(KerberosSettings::getKeytabPath, KerberosSettings::setKeytabPath);

        servicePrincipalField = new ComboBox<>("Service Principal");
        servicePrincipalField.setWidthFull();
        servicePrincipalField.setItems("");
        kerberosBinder.forField(servicePrincipalField)
                .withValidator(new IgnoringInvisibleOrDisabledValidator<>(
                        new ServicePrincipalValidator()
                ))
                .bind(KerberosSettings::getServicePrincipal, KerberosSettings::setServicePrincipal);

        readKeytabButton = new Button(
                "Read Entries from Keytab",
                e -> {
                    String filename = keytabPathField.getValue();
                    try {
                        KeytabFile keytabFile = new KeytabFile(filename);
                        Set<String> principals = keytabFile.getPrincipals();
                        servicePrincipalField.setItems(principals);
                        logger.info(
                                "Found principals in %s: %s"
                                        .formatted(filename, principals)
                        );
                        Optional<String> principal = principals.stream().findFirst();
                        if (principal.isPresent()) {
                            servicePrincipalField.setValue(principal.get());
                        }
                        kerberosBinder.validate();
                    } catch (IOException | KeytabException ex) {
                        String header = "Cannot read %s: %s"
                                .formatted(filename, ex.getMessage()
                                );
                        logger.error(header + ex.getMessage());
                        ShowNotification.error(header, ex.getMessage());
                    }
                    kerberosBinder.validate();
                }
        );

        HorizontalLayout servicePrincipalLayout = new HorizontalLayout(
                servicePrincipalField,
                readKeytabButton
        );
        servicePrincipalLayout.setFlexGrow(1, servicePrincipalField);
        servicePrincipalLayout.setWidthFull();
        servicePrincipalLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        kerberosBinder.addStatusChangeListener(
                l -> {
                    saveButton.setEnabled(
                            !enableKerberosAuthField.getValue()
                            || !l.hasValidationErrors()
                    );
                }
        );

        enableKerberosAuthField.addValueChangeListener((e) -> {
            onUpdateEnableKerberos(e.getValue());
        });

        VerticalLayout layout = new VerticalLayout(
                enableKerberosAuthField,
                keytabPathField,
                servicePrincipalLayout
        );
        layout.setMaxWidth(50, Unit.EM);

        onUpdateEnableKerberos(enableKerberosAuthField.getValue());

        return layout;
    }

    private Component createPreAuthView(Settings settings) {
        PreAuthSettings preAuthSettings = settings.getSettings(PreAuthSettings.class);
        preAuthBinder = new Binder<>();
        preAuthBinder.setBean(preAuthSettings);

        preAuthEnabled = new Checkbox("Enable Pre Authentication");
        preAuthBinder
                .forField(preAuthEnabled)
                .bind(PreAuthSettings::isPreAuthtEnabled, PreAuthSettings::setPreAuthtEnabled);

        preAuthEnvVar = new TextField("Environment Variable");
        preAuthEnvVar.setWidthFull();
        preAuthEnvVar.setClearButtonVisible(true);
        preAuthBinder
                .forField(preAuthEnvVar)
                .asRequired()
                .bind(PreAuthSettings::getEnvironmentVariable, PreAuthSettings::setEnvironmentVariable);

        preAuthHttpHeader = new TextField("HTTP Header");
        preAuthHttpHeader.setWidthFull();
        preAuthHttpHeader.setClearButtonVisible(true);
        preAuthBinder
                .forField(preAuthHttpHeader)
                .asRequired()
                .bind(PreAuthSettings::getHttpHeader, PreAuthSettings::setHttpHeader);

        preAuthSource = new RadioButtonGroup<>();
        preAuthSource.setLabel("Authentication Source");
        preAuthSource.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        preAuthSource.setItems(PreAuthSettings.PreAuthSource.values());
        preAuthSource.setWidthFull();
        preAuthSource.addValueChangeListener((e) -> {
            switch (e.getValue()) {
                case ENVIRONMENT_VARIABLE -> {
                    preAuthEnvVar.setVisible(true);
                    preAuthHttpHeader.setVisible(false);
                }
                case HTTP_HEADER -> {
                    preAuthEnvVar.setVisible(false);
                    preAuthHttpHeader.setVisible(true);
                }
            }
        });
        preAuthBinder.forField(preAuthSource)
                .bind(PreAuthSettings::getPreAuthSource, PreAuthSettings::setPreAuthSource);

        createApacheConfig = new Checkbox("Create Apache Configuration");
        preAuthBinder
                .forField(createApacheConfig)
                .bind(PreAuthSettings::isWriteApachePreAuthConfig, PreAuthSettings::setWriteApachePreAuthConfig);

        apacheKeytabFile = new TextField("Keytab Location");
        apacheKeytabFile.setWidthFull();
        apacheKeytabFile.setClearButtonVisible(true);
        preAuthBinder
                .forField(apacheKeytabFile)
                .bind(PreAuthSettings::getKeytabFile, PreAuthSettings::setKeytabFile);

        preAuthEnabled.addValueChangeListener((e) -> {
            onEnablePreAuthentication(e.getValue());
        });

        createApacheConfig.addValueChangeListener((e) -> {
            apacheKeytabFile.setEnabled(createApacheConfig.getValue());
        });

        VerticalLayout layout = new VerticalLayout(
                preAuthEnabled,
                preAuthSource,
                preAuthEnvVar,
                preAuthHttpHeader,
                createApacheConfig,
                apacheKeytabFile
        );
        layout.setMaxWidth(50, Unit.EM);

        preAuthBinder.validate();
        onEnablePreAuthentication(preAuthEnabled.getValue());

        return layout;
    }

    private void onUpdateEnableKerberos(boolean enabled) {
        keytabPathField.setEnabled(enabled);
        servicePrincipalField.setEnabled(enabled);
        readKeytabButton.setEnabled(enabled);
        kerberosBinder.validate();
    }

    private void onEnablePreAuthentication(boolean enable) {
        preAuthSource.setEnabled(enable);
        preAuthEnvVar.setEnabled(enable);
        createApacheConfig.setEnabled(enable);
        apacheKeytabFile.setEnabled(enable && createApacheConfig.getValue());
        preAuthBinder.validate();
    }

    private void onSaveAndRestart() {
        KerberosSettings kerberosSettings = kerberosBinder.getBean();
        PreAuthSettings preAuthSettings = preAuthBinder.getBean();
        try {
            kerberosSettings.save(settings);
            preAuthSettings.save(settings);
            if (preAuthSettings.isWriteApachePreAuthConfig()) {
                tomcatService.saveApacheConfig();
            }
            Notification.show("Restarting Arachne");
            Arachne.restart();
        } catch (SettingsException ex) {
            logger.error(ex.getMessage());
        }
    }
}
