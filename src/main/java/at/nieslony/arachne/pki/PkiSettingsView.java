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
package at.nieslony.arachne.pki;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.openvpn.OpenVpnRestController;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "pki", layout = ViewTemplate.class)
@PageTitle("Pki | Arachne")
@RolesAllowed("ADMIN")
public class PkiSettingsView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(PkiSettingsView.class);

    private Binder<PkiSettings> binder;
    private Dialog writeDhParamsDialog;

    public PkiSettingsView(
            Settings settings,
            OpenVpnRestController openVpnRestController
    ) {
        PkiSettings pkiSettings = settings.getSettings(PkiSettings.class);
        binder = new Binder<>();
        writeDhParamsDialog = createWriteDhParamsDialog();

        Select<Integer> dhParamsBitsField = new Select<>();
        dhParamsBitsField.setLabel("DH Params Bits");
        dhParamsBitsField.setItems(1024, 2048, 4096);
        binder.bind(
                dhParamsBitsField,
                PkiSettings::getDhParamsBits,
                PkiSettings::setDhParamsBits
        );

        Button createDhParamsButton = new Button("Recreate DH Params", (e) -> {
            writeDhParamsDialog.open();
        });

        HorizontalLayout dhParamsLayout = new HorizontalLayout(
                dhParamsBitsField,
                createDhParamsButton
        );
        dhParamsLayout.setAlignItems(Alignment.BASELINE);

        IntegerField crlLifetimeDaysField = new IntegerField("CRL Lifetime");
        crlLifetimeDaysField.setStepButtonsVisible(true);
        crlLifetimeDaysField.setSuffixComponent(new Div(new Text("days")));
        binder.bind(
                crlLifetimeDaysField,
                PkiSettings::getCrlLifeTimeDays,
                PkiSettings::setCrlLifeTimeDays
        );

        IntegerField serverCertRenewDaysField
                = new IntegerField("Renew Server Certificate before Expiration");
        serverCertRenewDaysField.setStepButtonsVisible(true);
        serverCertRenewDaysField.setSuffixComponent(new Div(new Text("days")));
        binder.bind(
                serverCertRenewDaysField,
                PkiSettings::getServerCertRenewDays,
                PkiSettings::setServerCertRenewDays
        );

        Button createCrlButton = new Button("Recreate CRL", (e) -> {
            openVpnRestController.writeCrl();
            Notification.show("CRL written");
        });

        HorizontalLayout crlLayout = new HorizontalLayout(
                crlLifetimeDaysField,
                createCrlButton,
                serverCertRenewDaysField
        );
        crlLayout.setAlignItems(Alignment.BASELINE);

        Button saveButton = new Button("Save", (e) -> {
            try {
                binder.getBean().save(settings);
            } catch (SettingsException ex) {
                logger.error("Cannot save pki settings: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(
                dhParamsLayout,
                crlLayout,
                saveButton
        );

        binder.setBean(pkiSettings);
    }

    private Dialog createWriteDhParamsDialog() {
        Dialog dlg = new Dialog("Create DH Parameters");
        Text msg = new Text("Create DH parameters in background");

        Checkbox restartOpenVpnServerField
                = new Checkbox("Write openVPN server config and restart openVPN");

        VerticalLayout layout = new VerticalLayout(
                msg,
                restartOpenVpnServerField
        );
        layout.setMargin(false);
        dlg.add(layout);

        Button okButton = new Button("OK", (e) -> {
            dlg.close();
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        dlg.getFooter().add(
                okButton,
                cancelButton
        );

        return dlg;
    }
}
