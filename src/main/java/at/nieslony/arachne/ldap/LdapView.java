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
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
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
public class LdapView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(LdapView.class);

    private final Settings settings;

    public LdapView(Settings settings) {
        this.settings = settings;

        Binder<LdapSettings> binder = new Binder<>(LdapSettings.class);
        LdapSettings ldapSettings = new LdapSettings(settings);

        Select<LdapSettings.LdapProtocol> protocolField = new Select<>();
        protocolField.setLabel("Protocol");
        protocolField.setItems(LdapSettings.LdapProtocol.LDAP, LdapSettings.LdapProtocol.LDAPS);
        protocolField.setValue(LdapSettings.LdapProtocol.LDAP);
        binder.forField(protocolField)
                .asRequired("Value Required")
                .bind(LdapSettings::getProtocol, LdapSettings::setProtocol);

        TextField hostField = new TextField("Host");
        binder.forField(hostField)
                .asRequired("Value required")
                .bind(LdapSettings::getHost, LdapSettings::setHost);

        IntegerField portField = new IntegerField("Port");
        portField.setMin(1);
        portField.setMax(65534);
        portField.setValue(LdapSettings.LdapProtocol.LDAP.getPort());
        binder.forField(portField)
                .asRequired("Value required")
                .bind(LdapSettings::getPort, LdapSettings::setPort);

        TextField baseDnField = new TextField("Base DN");
        binder.forField(baseDnField)
                .asRequired("Value Required")
                .bind(LdapSettings::getBaseDn, LdapSettings::setBaseDn);

        RadioButtonGroup<LdapSettings.LdapBindType> bindTypeField = new RadioButtonGroup<>();
        bindTypeField.setLabel("Bind Type");
        bindTypeField.setItems(LdapSettings.LdapBindType.values());
        binder.forField(bindTypeField)
                .bind(LdapSettings::getBindType, LdapSettings::setBindType);

        HorizontalLayout hostSettings = new HorizontalLayout(
                protocolField,
                new Text("://"),
                hostField,
                new Text(":"),
                portField
        );
        hostSettings.setJustifyContentMode(JustifyContentMode.CENTER);

        HorizontalLayout bindDnSettings = new HorizontalLayout();
        TextField bindDnField = new TextField("Bind DN");
        PasswordField bindPasswordField = new PasswordField("Password");
        bindDnSettings.add(bindDnField, bindPasswordField);

        TextField kerberosKeytabField = new TextField("Keytab");

        Button testLdapBindButton = new Button(
                "Test LDAP Bind",
                e -> testLdapConnection(ldapSettings)
        );

        protocolField.addValueChangeListener((event) -> {
            int selectedPort = portField.getValue();
            if (selectedPort == LdapSettings.LdapProtocol.LDAP.getPort()
                    || selectedPort == LdapSettings.LdapProtocol.LDAPS.getPort()) {
                portField.setValue(event.getValue().getPort());
            }
        });

        bindTypeField.addValueChangeListener(e -> {
            switch (e.getValue()) {
                case ANONYMOUS -> {
                    bindDnSettings.setVisible(false);
                    kerberosKeytabField.setVisible(false);
                }
                case BIND_DN -> {
                    bindDnSettings.setVisible(true);
                    kerberosKeytabField.setVisible(false);
                }
                case KEYTAB -> {
                    bindDnSettings.setVisible(false);
                    kerberosKeytabField.setVisible(true);
                }
            }
        });

        binder.setBean(ldapSettings);
        binder.validate();

        add(
                hostSettings,
                baseDnField,
                bindTypeField,
                bindDnSettings,
                kerberosKeytabField,
                testLdapBindButton
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
}
