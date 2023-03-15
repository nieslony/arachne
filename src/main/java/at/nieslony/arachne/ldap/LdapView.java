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
package at.nieslony.arachne.ldap;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.HostnameValidator;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 *
 * @author claas
 */
@Route(value = "ldap_settings", layout = ViewTemplate.class)
@PageTitle("LDAP Settings | Arachne")
@RolesAllowed("ADMIN")
public class LdapView extends FormLayout {

    private static final Logger logger = LoggerFactory.getLogger(LdapView.class);

    private final Settings settings;

    public LdapView(Settings settings) {
        this.settings = settings;
        LdapSettings ldapSettings = new LdapSettings(settings);
        Binder<LdapSettings> binder = new Binder<>();

        var ldapUrlsEditor = createUrlsEditor(ldapSettings);

        TextField baseDnField = new TextField("Base DN");
        baseDnField.setWidthFull();
        binder.forField(baseDnField)
                .asRequired("Value Required")
                .bind(LdapSettings::getBaseDn, LdapSettings::setBaseDn);

        RadioButtonGroup<LdapSettings.LdapBindType> bindType
                = new RadioButtonGroup<>("Authentication Type");
        bindType.setItems(LdapSettings.LdapBindType.values());
        binder.forField(bindType)
                .bind(LdapSettings::getBindType, LdapSettings::setBindType);

        TextField bindDnField = new TextField("Bind DN");
        bindDnField.setWidthFull();
        PasswordField bindPasswordField = new PasswordField("Bind Password");
        bindPasswordField.setWidthFull();
        HorizontalLayout simpleBindLayout = new HorizontalLayout(
                bindDnField,
                bindPasswordField
        );
        simpleBindLayout.setWidthFull();
        binder.forField(bindDnField)
                .bind(LdapSettings::getBindDn, LdapSettings::setBindDn);

        TextField keytabPath = new TextField("Keytab Path");
        binder.forField(keytabPath)
                .bind(LdapSettings::getKeytabPath, LdapSettings::setKeytabPath);

        bindType.addValueChangeListener(e -> {
            switch (bindType.getValue()) {
                case ANONYMOUS -> {
                    bindDnField.setVisible(false);
                    bindPasswordField.setVisible(false);
                    keytabPath.setVisible(false);
                }
                case BIND_DN -> {
                    bindDnField.setVisible(true);
                    bindPasswordField.setVisible(true);
                    keytabPath.setVisible(false);
                }
                case KEYTAB -> {
                    bindDnField.setVisible(false);
                    bindPasswordField.setVisible(false);
                    keytabPath.setVisible(true);
                }

            }
        });

        binder.setBean(ldapSettings);
        binder.validate();

        add(
                ldapUrlsEditor,
                new VerticalLayout(
                        baseDnField,
                        bindType,
                        simpleBindLayout,
                        keytabPath
                )
        );
    }

    void testLdapConnection(LdapSettings ldapSettings) {
        /*   KerberosLdapContextSource contextSource = new KerberosLdapContextSource("");
         */
        LdapContextSource ctxSrc = new LdapContextSource();
        ctxSrc.setUrl("ldap://<ldapUrl>:389");
        ctxSrc.setBase("DC=bar,DC=test,DC=foo");
        ctxSrc.setUserDn("CN=<...>, DC=bar, DC=test, DC=foo");
        ctxSrc.setPassword("<password>");

        ctxSrc.afterPropertiesSet();

    }

    VerticalLayout createUrlsEditor(LdapSettings ldapSettings) {
        Binder binder = new Binder();

        ListBox<LdapUrl> ldapUrlsField = new ListBox<>();
        ldapUrlsField.setHeight(24, Unit.EX);
        ldapUrlsField.setItems(ldapSettings.getLdapUrls());
        ldapUrlsField.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        ldapUrlsField.setWidthFull();

        Select<LdapProtocol> protocolField = new Select<>();
        protocolField.setLabel("Protocol");
        protocolField.setItems(LdapProtocol.values());
        protocolField.setWidth(6, Unit.EM);
        protocolField.setValue(LdapProtocol.LDAP);

        TextField hostnameField = new TextField("LDAP Host");
        hostnameField.setMinWidth(24, Unit.EX);
        hostnameField.setPattern(
                "[a-z][a-z0-9\\-]*(\\.[a-z][a-z0-9\\-]*)*"
        );
        AtomicReference<String> hostname = new AtomicReference<>("");
        binder.forField(hostnameField)
                .withValidator(new HostnameValidator(), "Not a valid hostname")
                .bind(
                        s -> hostname.get(),
                        (s, v) -> {
                            hostname.set(v.toString());
                        }
                );
        hostnameField.setValueChangeMode(ValueChangeMode.EAGER);

        IntegerField portField = new IntegerField("Port");
        portField.setMin(1);
        portField.setMax(65534);
        portField.setStepButtonsVisible(true);
        portField.setWidth(8, Unit.EM);
        portField.setValue(389);

        HorizontalLayout urlsLayout = new HorizontalLayout(
                protocolField,
                hostnameField,
                portField);
        urlsLayout.setFlexGrow(1, hostnameField);
        urlsLayout.setWidthFull();

        Button addUrlButton = new Button(
                "Add",
                e -> {
                    List<LdapUrl> ldapUrls = new LinkedList<>(ldapSettings.getLdapUrls());
                    logger.info(ldapUrls.toString());
                    ldapUrls.add(
                            new LdapUrl(
                                    protocolField.getValue(),
                                    hostnameField.getValue(),
                                    portField.getValue()
                            )
                    );
                    logger.info(ldapUrls.toString());
                    ldapUrlsField.setItems(ldapUrls);
                    ldapSettings.setLdapUrls(ldapUrls);
                });
        Button updateUrlButton = new Button(
                "Update",
                e -> {
                    List<LdapUrl> ldapUrls = new LinkedList<>(ldapSettings.getLdapUrls());
                    ldapUrls.remove(ldapUrlsField.getValue());
                    ldapUrls.add(
                            new LdapUrl(
                                    protocolField.getValue(),
                                    hostnameField.getValue(),
                                    portField.getValue()
                            )
                    );
                    ldapUrlsField.setItems(ldapUrls);
                    ldapSettings.setLdapUrls(ldapUrls);
                });
        Button removeUrlButton = new Button(
                "Remove", e -> {
                    List<LdapUrl> ldapUrls = new LinkedList<>(ldapSettings.getLdapUrls());
                    ldapUrls.remove(ldapUrlsField.getValue());
                    ldapUrlsField.setItems(ldapUrls);
                    ldapSettings.setLdapUrls(ldapUrls);
                });
        HorizontalLayout buttonsLayout = new HorizontalLayout(
                addUrlButton,
                updateUrlButton,
                removeUrlButton
        );

        ldapUrlsField.addValueChangeListener(e -> {
            LdapUrl url = ldapUrlsField.getValue();
            if (url != null) {
                protocolField.setValue(url.getProtocol());
                hostnameField.setValue(url.getHost());
                portField.setValue(url.getPort());
            }
        });

        hostnameField.addValueChangeListener(e -> {
            if (e.getValue().isEmpty()) {
                addUrlButton.setEnabled(false);
                updateUrlButton.setEnabled(false);
            } else {
                addUrlButton.setEnabled(true);
                updateUrlButton.setEnabled(true);
            }
        });

        VerticalLayout layout = new VerticalLayout(
                new Label("Try LDAP URLs"),
                ldapUrlsField,
                urlsLayout,
                buttonsLayout
        );
        layout.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM
        );

        return layout;
    }

    boolean ldapUrlExists(
            LdapSettings ldapSettings,
            LdapProtocol protocol,
            String hostname,
            int port) {
        LdapUrl url = new LdapUrl(protocol, hostname, port);
        return !ldapSettings.getLdapUrls().contains(url);
    }
}
