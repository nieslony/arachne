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
package at.nieslony.arachne.mail;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.settings.Settings;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/**
 *
 * @author claas
 */
@Route(value = "mail_settings", layout = ViewTemplate.class)
@PageTitle("E-Mail Settings | Arachne")
@RolesAllowed("ADMIN")
public class MailSettingsView extends FormLayout {

    private static final Logger logger = LoggerFactory.getLogger(MailSettingsView.class);

    private final Settings settings;

    public MailSettingsView(Settings settings) {
        this.settings = settings;
        MailSettings mailSettings = new MailSettings(this.settings);
        Binder<MailSettings> binder = new Binder();

        TextField smtpServerField = new TextField("SMTP Server");
        binder.forField(smtpServerField)
                .asRequired("SMTP Server is requird")
                .bind(MailSettings::getSmtpServer, MailSettings::setSmtpServer);

        ComboBox<Integer> smtpPortField = new ComboBox<>("SMTP Port");
        smtpPortField.setItems(25, 587);
        smtpPortField.setAllowCustomValue(true);
        smtpPortField.setAllowedCharPattern("[0-9]");
        smtpPortField.setRequired(true);
        smtpPortField.addCustomValueSetListener((t) -> {
            String value = t.getDetail();
            smtpPortField.setValue(Integer.valueOf(value));
        });
        smtpPortField.setRenderer(new ComponentRenderer<>(
                value -> {
                    if (value == null) {
                        return new Text("");
                    }
                    return new Text(
                            switch (value) {
                        case 25 ->
                            "25 - SMTP";
                        case 587 ->
                            "587 - Submission";
                        default ->
                            value.toString();
                    });
                }));
        binder.forField(smtpPortField)
                .asRequired("SMTP Port is required")
                .withValidator((t) -> t > 0 && t < 65536, "Valid range: 1...65535")
                .bind(MailSettings::getSmtpPort, MailSettings::setSmtpPort);

        TextField smtpUserField = new TextField("SMTP User");
        binder.forField(smtpUserField)
                .bind(MailSettings::getSmtpUser, MailSettings::setSmtpUser);

        PasswordField smtpPasswordField = new PasswordField("SMTP Password");
        binder.forField(smtpPasswordField)
                .bind(MailSettings::getSmtpPassword, MailSettings::setSmtpPassword);

        TextField senderDisplayNameField = new TextField("Sender Displayname");
        binder.forField(senderDisplayNameField)
                .bind(MailSettings::getSenderDisplayname, MailSettings::setSenderDisplayname);

        EmailField senderEmailAddressField = new EmailField("Sender E-Mail Address");
        binder.forField(senderEmailAddressField)
                .bind(MailSettings::getSenderEmailAddress, MailSettings::setSenderEmailAddress);

        TextArea templateContentField = new TextArea("Send Config Mail Template");
        templateContentField.setHelperText("Add fields: displayname sendername");
        binder.forField(templateContentField)
                .asRequired("Mail Template cannot be empty")
                .bind(MailSettings::getTemplateConfig, MailSettings::setTemplateConfig);

        Html templatePreview = new Html("<div></div>");

        templateContentField.addValueChangeListener((e) -> {
            templatePreview.setHtmlContent("<div>%s</div>".formatted(e.getValue()));
        });

        Button sendTestMail = new Button("Send Test Mail", (e) -> {
            Dialog dlg = new Dialog();
            dlg.setHeaderTitle("Send Test Mail");

            EmailField recipiend = new EmailField("Recipient");
            recipiend.setWidth(20, Unit.EM);
            recipiend.setRequired(true);
            recipiend.setValueChangeMode(ValueChangeMode.EAGER);
            recipiend.setErrorMessage("Not a valif E-Mail Address");
            dlg.add(recipiend);

            Button cancelButton = new Button("Cancel", (be) -> dlg.close());
            Button sendButton = new Button("Send", (be) -> {
                sendTestMail(mailSettings, recipiend.getValue());
                dlg.close();
            });
            sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            sendButton.setAutofocus(true);
            sendButton.setDisableOnClick(true);
            sendButton.setEnabled(false);

            dlg.getFooter().add(cancelButton, sendButton);

            recipiend.addValueChangeListener((var vce) -> {
                sendButton.setEnabled(
                        !vce.getValue().isEmpty()
                        && !recipiend.isInvalid()
                );
            });

            dlg.open();
        });

        Button saveButton = new Button("Save", (e) -> {
            mailSettings.save(settings);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setAutofocus(true);

        HorizontalLayout buttonLayout = new HorizontalLayout(
                sendTestMail,
                saveButton
        );

        binder.setBean(mailSettings);
        binder.validate();
        add(smtpServerField,
                smtpPortField,
                smtpUserField,
                smtpPasswordField,
                senderDisplayNameField,
                senderEmailAddressField,
                templateContentField,
                templatePreview,
                buttonLayout
        );
    }

    private void sendTestMail(MailSettings mailSettings, String to) {
        MailSender mailSender = mailSettings.getMailSender();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailSettings.getPrettySenderMailAddress());
        message.setSubject("Arachne Test Mail");
        message.setTo(to);
        message.setText(
                """
                Dear %s,

                this is a test mail. Please ignore.

                Best regards
                %s
                """.formatted(to, mailSettings.getSenderDisplayname()));

        try {
            mailSender.send(message);
            Notification.show("Test Mail sent.");
        } catch (MailException ex) {
            String msg = "Cannot send Test Mail: " + ex.getMessage();
            logger.error(msg);
            Notification notification = Notification.show(msg);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
