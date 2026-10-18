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
package at.nieslony.arachne.webserver;

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
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author claas
 */
@Route(value = "webserver", layout = ViewTemplate.class)
@PageTitle("Webserver")
@RolesAllowed("ADMIN")
@Slf4j
public class WebserverView extends TabSheet {

    @Value("${tomcatCertPath:${arachneConfigDir}/server.crt}")
    String tomcatCertPath;

    @Value("${tomcatKeyPath:${arachneConfigDir}/server.key}")
    String tomcatKeyPath;

    @Autowired
    private Pki pki;

    @Autowired
    private Settings settings;

    @Autowired
    private ReverseProxyService reverseProxyService;

    @Autowired
    private ReverseProxyService tomcatService;

    @Autowired
    private FolderFactory folderFactory;

    private TomcatSettings tomcatSettings;
    Binder<TomcatSettings> tomcatBinder = new Binder<>();
    Binder<ReverseProxySettings> reverseProxyBinder = new Binder<>();

    public WebserverView() {
        this.tomcatBinder = new Binder<>();
    }

    @PostConstruct
    public void init() {
        add("Integrated Tomcat", createTomcatView());
        add("Reverse Proxy", createReverseProxyView());
        setWidthFull();
    }

    public Component createReverseProxyView() {
        ReverseProxySettings reverseProxySettings = settings.getSettings(ReverseProxySettings.class);
        VerticalLayout layout = new VerticalLayout();

        TextField reverseProxyPath = new TextField("Reverse Proxy Path");
        reverseProxyPath.setClearButtonVisible(true);

        Select<ReverseProxySettings.AddAuthentication> addAuthentication
                = new Select<>("Add Authentication");
        addAuthentication.setItems(ReverseProxySettings.AddAuthentication.values());

        TextField kerberosKeytab = new TextField("Kerberos Keytab");
        kerberosKeytab.setClearButtonVisible(true);
        kerberosKeytab.setEnabled(false);

        Button writeConfigButton = new Button("Write Config", e -> {
            try {
                reverseProxySettings.save(settings);
                reverseProxyService.saveApacheConfig();
            } catch (SettingsException ex) {

            }
        });
        writeConfigButton.addThemeVariants(ButtonVariant.PRIMARY);

        layout.add(
                reverseProxyPath,
                addAuthentication,
                kerberosKeytab,
                writeConfigButton
        );

        addAuthentication.addValueChangeListener(
                e -> kerberosKeytab.setEnabled(
                        e.getValue() == ReverseProxySettings.AddAuthentication.KERBEROS)
        );

        reverseProxyBinder.bind(
                reverseProxyPath,
                ReverseProxySettings::getProxyPath,
                ReverseProxySettings::setProxyPath
        );
        reverseProxyBinder.bind(
                addAuthentication,
                ReverseProxySettings::getAddAuthentication,
                ReverseProxySettings::setAddAuthentication
        );
        reverseProxyBinder.bind(
                kerberosKeytab,
                ReverseProxySettings::getKeytabFile,
                ReverseProxySettings::setKeytabFile
        );
        reverseProxyBinder.setBean(reverseProxySettings);

        return layout;
    }

    public Component createTomcatView() {
        tomcatSettings = settings.getSettings(TomcatSettings.class);

        Button saveAndRestartButton = new Button(
                "Save and Restart Arachne",
                e -> onSave()
        );
        saveAndRestartButton.setDisableOnClick(true);
        saveAndRestartButton.addThemeVariants(ButtonVariant.PRIMARY);

        VerticalLayout layout = new VerticalLayout(
                createAjpDetails(),
                createHttpsDetails(),
                saveAndRestartButton
        );
        layout.setPadding(false);

        tomcatBinder.setBean(tomcatSettings);
        tomcatBinder.validate();

        return layout;
    }

    private Component createAjpDetails() {

        Switch enableAjpField = new Switch("Enable AJP Connector");
        enableAjpField.setValue(true);

        IntegerField ajpPortField = new IntegerField("AJP Port");
        ajpPortField.setMin(1);
        ajpPortField.setMax(65535);
        ajpPortField.setStepButtonsVisible(true);

        Checkbox enableAjpSecretField = new Checkbox("Enable AJP Secret");
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

        tomcatBinder.forField(enableAjpField)
                .bind(TomcatSettings::isEnableAjpConnector, TomcatSettings::setEnableAjpConnector);
        tomcatBinder.forField(ajpPortField)
                .asRequired()
                .bind(TomcatSettings::getAjpPort, TomcatSettings::setAjpPort);
        tomcatBinder.forField(enableAjpSecretField)
                .bind(TomcatSettings::isEnableAjpSecret, TomcatSettings::setEnableAjpSecret);
        tomcatBinder.forField(ajpSecretField)
                .asRequired()
                .bind(TomcatSettings::getAjpSecret, TomcatSettings::setAjpSecret);

        enableAjpField.addValueChangeListener(
                e -> {
                    if (e.getValue()) {
                        ajpPortField.setEnabled(true);
                        enableAjpSecretField.setEnabled(true);
                        ajpSecretField.setEnabled(enableAjpSecretField.getValue() && enableAjpSecretField.getValue());
                        createSecret.setEnabled(true && enableAjpSecretField.getValue());
                    } else {
                        ajpPortField.setEnabled(false);
                        enableAjpSecretField.setEnabled(false);
                        ajpSecretField.setEnabled(false);
                        createSecret.setEnabled(false);
                    }
                });
        enableAjpSecretField.addValueChangeListener(
                e -> {
                    ajpSecretField.setEnabled(e.getValue());
                    createSecret.setEnabled(e.getValue());
                });

        Details ajpDetails = new Details("AJP Connector",
                new VerticalLayout(
                        enableAjpField,
                        ajpPortField,
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

            String absApacheConfigFN;
            try {
                absApacheConfigFN = new File(tomcatService.getApacheConfigFileName())
                        .getCanonicalPath();
            } catch (IOException ex) {
                absApacheConfigFN = "[%s is unresolvable: %s]"
                        .formatted(
                                tomcatService.getApacheConfigFileName(),
                                ex.getMessage()
                        );
            }

            Span apacheConfigFN = new Span(absApacheConfigFN);
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

        Switch httpsEnabledField = new Switch("Enable HTTPS");
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

        tomcatBinder.forField(httpsEnabledField)
                .bind(TomcatSettings::isHttpsConnectorEnabled, TomcatSettings::setHttpsConnectorEnabled);
        tomcatBinder.forField(httpsPortField)
                .bind(TomcatSettings::getHttpsPort, TomcatSettings::setHttpsPort);
        tomcatBinder.forField(httpsServerCertAsWebCertField)
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
            log.error(ex.getMessage());
            ShowNotification.error("Cannot write %s", ex.getRoorMessage());
        } catch (SettingsException | PkiException ex) {
            log.error("Cannot save tomcat settings: " + ex.getMessage());
        }
    }
}
