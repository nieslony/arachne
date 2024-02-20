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
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.io.StringWriter;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "cert-specs", layout = ViewTemplate.class)
@RolesAllowed("ADMIN")
public class CertSpecsView
        extends VerticalLayout
        implements HasUrlParameter<String>, HasDynamicTitle {

    private static final Logger logger = LoggerFactory.getLogger(CertSpecsView.class);

    private CertSpecs.CertSpecType certSpecType;
    private final Settings settings;
    private final Binder<CertSpecs> binder;
    private final TextField commonNameField;
    private final TextField organizationalUnitField;
    private final TextField organizationField;
    private final TextField countryField;
    private final TextField stateField;
    private final TextField locationField;
    private final TextField subjectField;

    public CertSpecsView(Settings settings) {
        this.settings = settings;
        this.binder = new Binder<>();

        commonNameField = new TextField("Common Name");
        organizationalUnitField = new TextField("Organizational Unit");
        organizationField = new TextField("Organization");
        countryField = new TextField("Country");
        stateField = new TextField("State");
        locationField = new TextField("Location");

        subjectField = new TextField("Subject");
        subjectField.setEnabled(false);
        binder.bind(subjectField, CertSpecs::getSubject, CertSpecs::setSubject);

        IntegerField certLifeTimeDaysField = new IntegerField("Lifetime");
        binder.bind(
                certLifeTimeDaysField,
                CertSpecs::getCertLifeTimeDays,
                CertSpecs::setCertLifeTimeDays
        );

        Select<String> keyAlgoField = new Select<>();
        keyAlgoField.setLabel("Key Algorithm");
        keyAlgoField.setItems("RSA");
        binder.bind(keyAlgoField, CertSpecs::getKeyAlgo, CertSpecs::setKeyAlgo);

        Select<String> rsaKeySizeField = new Select<>();
        rsaKeySizeField.setLabel("RSA Key Size");
        rsaKeySizeField.setItems("2048", "4096");
        binder.forField(rsaKeySizeField)
                .bind(
                        (source) -> {
                            return String.valueOf(source.getKeySize());
                        },
                        (source, value) -> {
                            source.setKeySize(Integer.valueOf(value));
                        }
                );

        Select<String> signatureAlgoField = new Select<>();
        signatureAlgoField.setLabel("Signature Algoirithm");
        signatureAlgoField.setItems("SHA256withRSA");
        binder.bind(
                signatureAlgoField,
                CertSpecs::getSignatureAlgo,
                CertSpecs::setSignatureAlgo
        );

        commonNameField.addValueChangeListener((e) -> updateSubject());
        organizationalUnitField.addValueChangeListener((e) -> updateSubject());
        organizationField.addValueChangeListener((e) -> updateSubject());
        countryField.addValueChangeListener((e) -> updateSubject());
        stateField.addValueChangeListener((e) -> updateSubject());
        locationField.addValueChangeListener((e) -> updateSubject());

        subjectField.addValueChangeListener(
                (e) -> onSubjectChanged(e.getValue())
        );

        FormLayout formLayout = new FormLayout(
                commonNameField,
                organizationalUnitField,
                organizationField,
                countryField,
                stateField,
                locationField,
                subjectField,
                certLifeTimeDaysField,
                keyAlgoField,
                rsaKeySizeField,
                signatureAlgoField
        );
        formLayout.setColspan(subjectField, 2);

        Button saveButton = new Button("Save", (e) -> {
            CertSpecs certSpecs = binder.getBean();
            if (certSpecs != null) {
                try {
                    certSpecs.save(settings);
                } catch (SettingsException ex) {
                    logger.error("Cannot save settings: " + ex.getMessage());
                }
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(
                formLayout,
                saveButton
        );
    }

    @Override
    public void setParameter(BeforeEvent be, String parameter) {
        try {
            certSpecType = CertSpecs.CertSpecType.valueOf(
                    parameter.toUpperCase().replaceAll("-", "_")
            );
        } catch (java.lang.IllegalArgumentException ex) {
            add(new Text("Invalid cert specification: " + parameter));
            return;
        }

        commonNameField.setValue("");
        organizationalUnitField.setValue("");
        organizationField.setValue("");
        countryField.setValue("");
        stateField.setValue("");
        locationField.setValue("");

        commonNameField.setEnabled(certSpecType != CertSpecs.CertSpecType.USER_SPEC);

        try {
            CertSpecs certSpecs = new CertSpecs(settings, certSpecType);
            binder.setBean(certSpecs);
        } catch (SettingsException ex) {
            logger.error("Cannot create cert specs: " + ex.getMessage());
        }
    }

    @Override
    public String getPageTitle() {
        if (certSpecType == null) {
            return "Ceritificate Specification";
        } else {
            return switch (certSpecType) {
                case CA_SPEC:
                    yield "Certificate Authority Specification";
                case SERVER_SPEC:
                    yield "Server Certificate Specification";
                case USER_SPEC:
                    yield "User Certificate Specification";
            };
        }
    }

    private void onSubjectChanged(String subjectStr) {
        X500Name subject = new X500Name(subjectStr);

        RDN cn = subject.getRDNs(BCStyle.CN)[0];
        commonNameField.setValue(cn.getFirst().getValue().toString());

        RDN[] ou = subject.getRDNs(BCStyle.OU);
        if (ou != null && ou.length > 0) {
            organizationalUnitField.setValue(ou[0].getFirst().getValue().toString());
        }

        RDN[] o = subject.getRDNs(BCStyle.O);
        if (o != null && o.length > 0) {
            organizationField.setValue(o[0].getFirst().getValue().toString());
        }

        RDN[] c = subject.getRDNs(BCStyle.C);
        if (c != null && c.length > 0) {
            countryField.setValue(c[0].getFirst().getValue().toString());
        }

        RDN[] st = subject.getRDNs(BCStyle.ST);
        if (st != null && st.length > 0) {
            stateField.setValue(st[0].getFirst().getValue().toString());
        }

        RDN[] l = subject.getRDNs(BCStyle.L);
        if (l != null && l.length > 0) {
            locationField.setValue(l[0].getFirst().getValue().toString());
        }
    }

    private void updateSubject() {
        StringWriter subjectWriter = new StringWriter();
        subjectWriter.append("CN=" + commonNameField.getValue());
        if (!organizationalUnitField.getValue().isEmpty()) {
            subjectWriter.append(",OU=" + organizationalUnitField.getValue());
        }
        if (!organizationField.getValue().isEmpty()) {
            subjectWriter.append(",o=" + organizationField.getValue());
        }
        if (!countryField.getValue().isEmpty()) {
            subjectWriter.append(",c=" + countryField.getValue());
        }
        if (!stateField.getValue().isEmpty()) {
            subjectWriter.append(",st=" + stateField.getValue());
        }
        if (!locationField.getValue().isEmpty()) {
            subjectWriter.append(",l=" + locationField.getValue());
        }

        X500Name subject = new X500Name(subjectWriter.toString());
        subjectField.setValue(subject.toString());
    }
}
