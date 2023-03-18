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
package at.nieslony.arachne.kerberos;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.settings.Settings;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
@PageTitle("Kerberos| Arachne")
@RolesAllowed("ADMIN")
public class KerberosView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(KerberosView.class);

    public KerberosView(Settings settings) {
        Notification notification = new Notification();
        notification.setDuration(5000);

        KerberosSettings kerberosSettings = new KerberosSettings(settings);
        Binder<KerberosSettings> binder = new Binder<>();
        binder.setBean(kerberosSettings);

        Checkbox enableKerberosAuthField = new Checkbox("Enable Kerberos Authentication");
        binder.forField(enableKerberosAuthField)
                .bind(KerberosSettings::isEnableKrbAuth, KerberosSettings::setEnableKrbAuth);

        TextField keytabPathField = new TextField("Keytab Path");
        keytabPathField.setWidthFull();
        binder.forField(keytabPathField)
                .withValidator((filename) -> {
                    File f = new File(filename);
                    return f.isFile() && f.canRead();
                }, "Cannot read file")
                .bind(KerberosSettings::getKeytabPath, KerberosSettings::setKeytabPath);

        ComboBox<String> servicePrincipalField = new ComboBox("Service Principal");
        servicePrincipalField.setWidthFull();
        servicePrincipalField.setItems("");
        binder.forField(servicePrincipalField)
                .asRequired("Principal expected")
                .bind(KerberosSettings::getServicePrincipal, KerberosSettings::setServicePrincipal);

        Button readKeytabButton = new Button(
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
                    } catch (IOException | KeytabException ex) {
                        String msg = "Cannot read %s: %s"
                                .formatted(filename, ex.getMessage());
                        logger.error(msg);
                        notification.add(msg);
                        notification.open();
                    }
                }
        );

        HorizontalLayout servicePrincipalLayout = new HorizontalLayout(
                servicePrincipalField,
                readKeytabButton
        );
        servicePrincipalLayout.setFlexGrow(1, servicePrincipalField);
        servicePrincipalLayout.setWidthFull();
        servicePrincipalLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        Button saveButton = new Button(
                "Save",
                e -> binder.getBean().save(settings)
        );

        binder.addStatusChangeListener(
                l -> {
                    saveButton.setEnabled(
                            !enableKerberosAuthField.getValue()
                            || !l.hasValidationErrors()
                    );
                }
        );

        add(
                enableKerberosAuthField,
                keytabPathField,
                servicePrincipalLayout,
                saveButton
        );
        setMaxWidth(50, Unit.EM);
    }
}
