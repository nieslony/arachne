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

import at.nieslony.arachne.Arachne;
import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.pki.UpdateWebServerCertificateException;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.utils.components.ShowNotification;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author claas
 */
@Route(value = "tomcat", layout = ViewTemplate.class)
@PageTitle("Integrated Tomcat")
@RolesAllowed("ADMIN")
public class TomcatView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(TomcatView.class);

    @Value("${tomcatCertPath:${arachneConfigDir}/server.crt}")
    String tomcatCertPath;

    @Value("${tomcatKeyPath:${arachneConfigDir}/server.key}")
    String tomcatKeyPath;

    @Autowired
    private Pki pki;

    private final Settings settings;
    private final TomcatService tomcatService;
    private final FolderFactory folderFactory;
    private final Binder<TomcatSettings> binder;
    private TomcatSettings tomcatSettings;

    public TomcatView(
            Settings settings,
            TomcatService tomcatService,
            FolderFactory folderFactory
    ) {
        this.settings = settings;

        this.tomcatService = tomcatService;
        this.folderFactory = folderFactory;
        this.binder = new Binder<>(TomcatSettings.class);
    }

    @PostConstruct
    public void init() {
        tomcatSettings = settings.getSettings(TomcatSettings.class);
        Button saveAndRestartButton = new Button(
                "Save and Restart Arachne",
                e -> onSave()
        );
        saveAndRestartButton.setDisableOnClick(true);
        saveAndRestartButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(
                createAjpDetails(),
                createHttpsDetails(),
                saveAndRestartButton
        );
        setPadding(false);

        binder.setBean(tomcatSettings);
        binder.validate();
    }

    private Component createAjpDetails() {

        Checkbox enableAjpField = new Checkbox("Enable AJP Connector");
        enableAjpField.setValue(true);

        IntegerField ajpPortField = new IntegerField("AJP Port");
        ajpPortField.setMin(1);
        ajpPortField.setMax(65535);
        ajpPortField.setStepButtonsVisible(true);

        TextField ajpLocationField = new TextField("Outsite Location");
        ajpLocationField.setClearButtonVisible(true);
        ajpLocationField.setWidthFull();

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
        HorizontalLayout ajpSecretLayout = new HorizontalLayout();
        ajpSecretLayout.add(
                enableAjpSecretField,
                ajpSecretField,
                createSecret
        );

        ajpSecretLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        ajpSecretLayout.setFlexGrow(1, ajpSecretField);
        ajpSecretLayout.setWidthFull();

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

        Details ajpDetails = new Details("AJP Connector",
                new VerticalLayout(
                        enableAjpField,
                        ajpPortField,
                        ajpLocationField,
                        ajpSecretLayout,
                        createApacheHint()
                )
        );
        ajpDetails.setOpened(true);
        ajpDetails.setMinWidth(50, Unit.EM);

        return ajpDetails;
    }

    private Component createApacheHint() {
        if (tomcatSettings.isEnableAjpConnector()) {
            Div msg = new Div();

            Span apacheConfigFN = new Span(tomcatService.getApacheConfigFileName());
            apacheConfigFN.getStyle().setFontWeight(Style.FontWeight.BOLD);

            Span apacheCfgFolder = new Span("/etc/httpd/conf.d");
            apacheCfgFolder.getStyle().setFontWeight(Style.FontWeight.BOLD);
            msg.add(
                    new Paragraph(
                            new Text("""
                                    You can also find a configuration for Apache
                                     HTTP Server at
                                    """
                            ),
                            apacheConfigFN,
                            new Text(".")
                    ),
                    new Paragraph(
                            new Text("""
                                  Please copy or symlink it to your apache
                                  configuration folder e.g.
                                     """),
                            apacheCfgFolder,
                            new Text(" and restart apache.")
                    )
            );
            return msg;
        } else {
            return new Text("");
        }
    }

    private Component createHttpsDetails() {

        Checkbox httpsEnabledField = new Checkbox("Enable HTTPS");
        httpsEnabledField.setValue(true);

        IntegerField httpsPortField = new IntegerField("HTTPS Port");
        httpsPortField.setMin(1);
        httpsPortField.setMax(65535);
        httpsPortField.setStepButtonsVisible(true);

        Checkbox httpsServerCertAsWebCertField = new Checkbox(
                "Use Server Certificate from Internal CA for Web Server"
        );
        HorizontalLayout certIsSymlinkHint = new HorizontalLayout(
                VaadinIcon.WARNING.create(),
                new Text(
                        """
                        %s/server.crt and/or %s/server.key exist but are not
                        files.
                        They cannot be replaced by internal certificate.
                        """.formatted(
                                folderFactory.getArachneConfigDir(),
                                folderFactory.getArachneConfigDir()
                        )
                )
        );
        if (!isCertWritable()) {
            httpsServerCertAsWebCertField.setEnabled(false);
        } else {
            certIsSymlinkHint.setVisible(false);
        }

        binder.forField(httpsEnabledField)
                .bind(TomcatSettings::isHttpConnectorEnabled, TomcatSettings::setHttpConnectorEnabled);
        binder.forField(httpsPortField)
                .bind(TomcatSettings::getHttpsPort, TomcatSettings::setHttpsPort);
        binder.forField(httpsServerCertAsWebCertField)
                .bind(TomcatSettings::isServerCertAsWebCert, TomcatSettings::setServerCertAsWebCert);

        httpsEnabledField.addValueChangeListener(
                e -> {
                    httpsPortField.setEnabled(e.getValue());
                }
        );

        Details httpsDetails = new Details("HTTPS Connector",
                new VerticalLayout(
                        httpsEnabledField,
                        httpsPortField,
                        httpsServerCertAsWebCertField,
                        certIsSymlinkHint
                )
        );
        httpsDetails.setOpened(true);

        return httpsDetails;
    }

    private boolean isCertWritable() {
        File certFile = new File(tomcatCertPath);
        File keyFile = new File(tomcatKeyPath);
        return (certFile.isFile() && keyFile.isFile())
                || (!certFile.exists() && !certFile.exists());
    }

    private void onSave() {
        try {
            pki.updateWebServerCertificate();
            tomcatSettings.save(settings);
            tomcatService.saveApacheConfig();
            Notification.show("Restarting Arachne...").open();
            Arachne.restart();
        } catch (UpdateWebServerCertificateException ex) {
            logger.error(ex.getMessage());
            ShowNotification.error("Cannot write %s", ex.getRoorMessage());
        } catch (SettingsException | PkiException ex) {
            logger.error("Cannot save tomcat settings: " + ex.getMessage());
        }
    }
}
