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
import static at.nieslony.arachne.mail.MailSettings.TemplateConfigType.HTML;
import static at.nieslony.arachne.mail.MailSettings.TemplateConfigType.PLAIN;
import at.nieslony.arachne.onetimeview.OneTimeViewService;
import at.nieslony.arachne.onetimeview.SetOtpView;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.roles.Role;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.UserRepository;
import at.nieslony.arachne.utils.components.ShowNotification;
import at.nieslony.arachne.utils.net.NetUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ErrorLevel;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorTheme;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author claas
 */
@Route(value = "mail-settings", layout = ViewTemplate.class)
@PageTitle("E-Mail Settings")
@RolesAllowed("ADMIN")
@Slf4j
public class MailSettingsView extends VerticalLayout {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailSettingsRestController mailSettingsRestController;

    @Autowired
    private Settings settings;

    @Autowired
    OneTimeViewService oneTimeViewService;

    private MailSettings mailSettings = null;

    private Dialog sendTestMailDialog;
    private Dialog sendTestConfigDialog;
    private Binder<MailSettings> binder;
    private Button sendTestMailButton;
    private Button sendTestConfigButton;
    private Button resetConfigTemplatesButton;
    private Button sendTestOtpAuthButton;
    private Button resetOtpAuthTemplatesButton;
    private HorizontalLayout buttons;

    @PostConstruct
    public void init() {
        mailSettings = settings.getSettings(MailSettings.class);
        binder = new Binder<>();
        sendTestMailDialog = createSendTestMailDialog();
        sendTestConfigDialog = createSendConfigDialog();

        Button saveButton = new Button("Save", (e) -> {
            try {
                mailSettings.save(settings);
            } catch (SettingsException ex) {
                log.error("Cannot save mail settings: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        buttons = new HorizontalLayout();
        buttons.add(saveButton);

        TabSheet tabs = new TabSheet();
        Tab basicsTab = new Tab("Basics");
        Tab templateConfigTab = new Tab("Template: Send Config");
        Tab templateOtpTab = new Tab("Template: Setup OTP Authenticator");
        tabs.add(basicsTab, createBasicsPage());
        tabs.add(templateConfigTab, createTemplateConfigPage());
        tabs.add(templateOtpTab, createTemplateOtpPage());
        tabs.setWidthFull();
        add(
                tabs,
                new Hr(),
                buttons
        );
        setPadding(false);

        tabs.setSelectedTab(null);
        tabs.addSelectedChangeListener((t) -> {
            sendTestMailButton.setVisible(false);
            sendTestConfigButton.setVisible(false);
            resetConfigTemplatesButton.setVisible(false);
            sendTestOtpAuthButton.setVisible(false);
            resetOtpAuthTemplatesButton.setVisible(false);
            if (t.getSelectedTab().equals(basicsTab)) {
                sendTestMailButton.setVisible(true);
            } else if (t.getSelectedTab().equals(templateConfigTab)) {
                sendTestConfigButton.setVisible(true);
                resetConfigTemplatesButton.setVisible(true);
            } else if (t.getSelectedTab().equals(templateOtpTab)) {
                sendTestOtpAuthButton.setVisible(true);
                resetOtpAuthTemplatesButton.setVisible(true);
            }
        });
        tabs.setSelectedTab(basicsTab);

        setMargin(false);
        setSpacing(false);
        setWidthFull();

        binder.setBean(mailSettings);
        binder.validate();
    }

    private Component createBasicsPage() {
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

        sendTestMailButton = new Button("Send Test Mail", (e) -> {
            sendTestMailDialog.open();
        });
        buttons.add(sendTestMailButton);

        FormLayout layout = new FormLayout(
                smtpServerField,
                smtpPortField,
                smtpUserField,
                smtpPasswordField,
                senderDisplayNameField,
                senderEmailAddressField
        );
        layout.setWidthFull();

        return layout;
    }

    private Dialog createSendTestOtpAuthDialog() {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Send Test OTH Auth Mail");

        EmailField recipiend = new EmailField("Recipient");
        recipiend.setWidth(20, Unit.EM);
        recipiend.setRequired(true);
        recipiend.setValueChangeMode(ValueChangeMode.EAGER);
        recipiend.setErrorMessage("Not a valid E-Mail Address");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserModel you = userRepository.findByUsername(authentication.getName());
        if (you != null && you.getEmail() != null) {
            recipiend.setValue(you.getEmail());
        }

        dlg.add(recipiend);

        Button cancelButton = new Button("Cancel", (be) -> dlg.close());
        Button sendButton = new Button("Send", (be) -> {
            try {
                oneTimeViewService.sendEmail(you, SetOtpView.class);
            } catch (MessagingException ex) {
                log.error("Cannot send email: " + ex.getMessage());
                ShowNotification.error("Cannot send e-mail", ex.getMessage());
            }
            dlg.close();
        });
        sendButton.addThemeVariants(ButtonVariant.PRIMARY);
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

        dlg.addOpenedChangeListener((t) -> {
            if (t.isOpened() && !recipiend.isEmpty() && !recipiend.isInvalid()) {
                sendButton.setEnabled(true);
            }
        });

        return dlg;
    }

    private Dialog createSendTestMailDialog() {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Send Test Mail");

        EmailField recipiend = new EmailField("Recipient");
        recipiend.setWidth(20, Unit.EM);
        recipiend.setRequired(true);
        recipiend.setValueChangeMode(ValueChangeMode.EAGER);
        recipiend.setErrorMessage("Not a valid E-Mail Address");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserModel you = userRepository.findByUsername(authentication.getName());
        if (you != null && you.getEmail() != null) {
            recipiend.setValue(you.getEmail());
        }

        dlg.add(recipiend);

        Button cancelButton = new Button("Cancel", (be) -> dlg.close());
        Button sendButton = new Button("Send", (be) -> {
            sendTestMail(recipiend.getValue());
            dlg.close();
        });
        sendButton.addThemeVariants(ButtonVariant.PRIMARY);
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

        dlg.addOpenedChangeListener((t) -> {
            if (t.isOpened() && !recipiend.isEmpty() && !recipiend.isInvalid()) {
                sendButton.setEnabled(true);
            }
        });

        return dlg;
    }

    private Dialog createSendConfigDialog() {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Send Mail with Configuration");

        EmailField recipiend = new EmailField("Recipient");
        recipiend.setWidth(20, Unit.EM);
        recipiend.setRequired(true);
        recipiend.setValueChangeMode(ValueChangeMode.EAGER);
        recipiend.setErrorMessage("Not a valid E-Mail Address");

        TextField configUser = new TextField("Send Confuguration for User");
        configUser.setWidth(20, Unit.EM);
        configUser.setRequired(true);
        configUser.setValueChangeMode(ValueChangeMode.EAGER);

        dlg.add(new VerticalLayout(
                recipiend,
                configUser
        ));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserModel you = userRepository.findByUsername(authentication.getName());
        if (you != null) {
            if (you.getEmail() != null) {
                recipiend.setValue(you.getEmail());
            }
            if (you.getRoles().contains(Role.USER.name())) {
                configUser.setValue(you.getUsername());
            }
        }

        Button cancelButton = new Button("Cancel", (be) -> dlg.close());
        Button sendButton = new Button("Send", (be) -> {
            String username = configUser.getValue();
            UserModel forUser = userRepository.findByUsername(username);
            if (!forUser.getRoles().contains(Role.USER.name())) {
                String msg = "User %s does not have role '%s', cannot sent config"
                        .formatted(username, Role.USER.toString());
                log.error(msg);
                ShowNotification.error("Error", msg);
            } else {
                sendTestConfig(forUser, recipiend.getValue());
            }
            dlg.close();
        });
        sendButton.addThemeVariants(ButtonVariant.PRIMARY);
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

        dlg.addOpenedChangeListener((t) -> {
            if (t.isOpened() && !recipiend.isEmpty() && !recipiend.isInvalid()) {
                sendButton.setEnabled(true);
            }
        });

        return dlg;
    }

    private Component createTemplateConfigPage() {
        RadioButtonGroup<MailSettings.TemplateConfigType> templateType
                = new RadioButtonGroup<>();
        templateType.setItems(MailSettings.TemplateConfigType.values());
        templateType.setValue(null);
        binder.bind(
                templateType,
                MailSettings::getTemplateConfigType,
                MailSettings::setTemplateConfigType
        );

        VaadinCKEditor templateContentHtmlField = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withType(CKEditorType.CLASSIC)
                .withTheme(CKEditorTheme.AUTO)
                .build();

        templateContentHtmlField.setWidthFull();
        templateContentHtmlField.addClassNames(
                LumoUtility.Background.CONTRAST_10
        );
        templateContentHtmlField.setVisible(false);
        binder.forField(templateContentHtmlField)
                .withValidator(contentValidator())
                .bind(
                        MailSettings::getTemplateConfigHtml,
                        MailSettings::setTemplateConfigHtml
                );

        TextArea templateContentPlainField = new TextArea();
        templateContentPlainField.setHeight(64, Unit.EX);
        templateContentPlainField.setWidthFull();
        binder.forField(templateContentPlainField)
                .withValidator(contentValidator())
                .bind(
                        MailSettings::getTemplateConfigPlain,
                        MailSettings::setTemplateConfigPlain
                );
        templateContentPlainField.setVisible(false);
        templateContentPlainField.getStyle().set("font-family", "monospace");

        VerticalLayout templateLayout = new VerticalLayout(
                templateType,
                templateContentHtmlField,
                templateContentPlainField
        );
        templateLayout.setDefaultHorizontalComponentAlignment(
                FlexComponent.Alignment.STRETCH
        );

        templateLayout.setWidthFull();
        templateLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        UnorderedList helper = new UnorderedList();
        helper.add(new ListItem(new Html(
                "<span><i>%s</i> Recipient's display name</span>"
                        .formatted(mailSettings.getVarRcptName())
        )));
        helper.add(new ListItem(new Html(
                "<span><i>%s</i> Sender's display name</span>"
                        .formatted(mailSettings.getVarSenderName())
        )));
        helper.add(new ListItem(new Html(
                "<span><i>%s</i> Linux instructions</span>"
                        .formatted(mailSettings.getVarLinuxInstructions())
        )));
        helper.add(new ListItem(new Html(
                "<span><i>%s</i> Network Manager Configuration Name</span>"
                        .formatted(mailSettings.getVarNmConnection())
        )));
        helper.add(new ListItem(new Html(
                "<span><i>%s</i> Attachment Name</span>"
                        .formatted(mailSettings.getVarAttachnement())
        )));

        VerticalLayout helperLayout = new VerticalLayout(
                new Text("Add the following place holders"),
                helper
        );
        helperLayout.setMargin(false);
        helperLayout.setSpacing(false);

        sendTestConfigButton = new Button("Send Test Configuration", (e) -> {
            sendTestConfigDialog.open();
        });
        resetConfigTemplatesButton = new Button("Load default Text", (e) -> {
            switch (templateType.getValue()) {
                case HTML ->
                    templateContentHtmlField.setValue(
                            mailSettings.getDefaultTemplateConfigHtml()
                    );
                case PLAIN ->
                    templateContentPlainField.setValue(
                            mailSettings.getDefaultTemplateConfigPlain()
                    );
            }
        });
        buttons.add(sendTestConfigButton, resetConfigTemplatesButton);

        templateType.addValueChangeListener((e) -> {
            (switch (e.getValue()) {
                case HTML:
                    yield templateContentHtmlField;
                case PLAIN:
                    yield templateContentPlainField;
            }).setVisible(true);
            if (e.getOldValue() != null) {
                (switch (e.getOldValue()) {
                    case HTML:
                        yield templateContentHtmlField;
                    case PLAIN:
                        yield templateContentPlainField;
                }).setVisible(false);
            }
        });

        HorizontalLayout layout = new HorizontalLayout(
                templateLayout,
                helperLayout
        );
        layout.setWidthFull();
        layout.setFlexShrink(1, templateLayout);
        layout.setFlexShrink(3, helperLayout);

        return layout;
    }

    private void sendTestConfig(UserModel forUser, String to) {
        try {
            mailSettingsRestController.sendConfigMail(
                    mailSettings,
                    forUser,
                    to,
                    "Arachne Test Mail with Configuration"
            );
        } catch (IOException | MessagingException | PkiException | SettingsException ex) {
            String header = "Cannot send Test Mail";
            log.error(header + ": " + ex.getMessage());
            ShowNotification.error(header, ex.getMessage());
        }
    }

    private void sendTestMail(String to) {
        JavaMailSender mailSender = mailSettings.getMailSender();
        SimpleMailMessage message = new SimpleMailMessage();
        String from = mailSettings.getPrettySenderMailAddress();
        message.setFrom(from);
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
            log.info("Sending Mail from %s to %s.".formatted(from, to));
            mailSender.send(message);
            String msg = "Test Mail sent from %s to %s.".formatted(from, to);
            log.info(msg);
            ShowNotification.info(msg);
        } catch (MailException ex) {
            String header = "Cannot send Test Mail";
            log.error(header + ": " + ex.getMessage());
            ShowNotification.error(header, ex.getMessage());
        }
    }

    private Validator<String> contentValidator() {
        return (String content, ValueContext vc) -> {
            Matcher m = Pattern.compile("\\{[^{}]*\\}")
                    .matcher(content);
            List<String> unknownVars
                    = m.results()
                            .map(MatchResult::group)
                            .filter((t)
                                    -> !t.equals(mailSettings.getVarSenderName())
                            )
                            .filter((t)
                                    -> !t.equals(mailSettings.getVarRcptName())
                            )
                            .filter((t)
                                    -> !t.equals(mailSettings.getVarNmConnection())
                            )
                            .filter((t)
                                    -> !t.equals(mailSettings.getVarLinuxInstructions())
                            )
                            .filter((t)
                                    -> !t.equals(mailSettings.getVarAttachnement())
                            )
                            .collect(Collectors.toList());

            if (!unknownVars.isEmpty()) {
                String msg = "Unknown place holders found: "
                        + String.join(", ", unknownVars);
                return ValidationResult.error(msg);
            }
            if (!content.contains(mailSettings.getVarLinuxInstructions())) {
                return ValidationResult.create(
                        "Dont't forget about Linux instructions",
                        ErrorLevel.WARNING);
            }
            return ValidationResult.ok();
        };
    }

    private Component createTemplateOtpPage() {
        VerticalLayout layout = new VerticalLayout();

        ComboBox<String> urlSetupOtpAuth = new ComboBox<>("URL to Authenticator Setup");
        urlSetupOtpAuth.setAllowCustomValue(true);
        List<String> items = List.of(
                Objects.requireNonNullElse(NetUtils.myHostname(), ""),
                Objects.requireNonNullElse(NetUtils.myPublicIpAddress(), ""),
                Objects.requireNonNullElse(NetUtils.myPublicHostname(), "")
        );
        urlSetupOtpAuth.setItems(items.stream()
                .filter(v -> !v.isEmpty())
                .sorted()
                .map(v -> "https://%s/arachne".formatted(v))
                .toList()
        );
        HorizontalLayout urlLayout = new HorizontalLayout(
                urlSetupOtpAuth,
                new Text("/otv")
        );
        urlLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        urlLayout.setFlexGrow(1, urlSetupOtpAuth);
        urlLayout.setWidthFull();

        RadioButtonGroup<MailSettings.TemplateConfigType> otpAuthTemplateType
                = new RadioButtonGroup<>();
        otpAuthTemplateType.setItems(MailSettings.TemplateConfigType.values());

        VaadinCKEditor editorHtml = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withType(CKEditorType.CLASSIC)
                .withTheme(CKEditorTheme.AUTO)
                .build();
        editorHtml.setHeight(64, Unit.EX);

        TextArea editorPlain = new TextArea();
        editorPlain.setHeight(64, Unit.EX);
        editorPlain.getStyle().set("font-family", "monospace");

        Div help = new Div(
                new Text("Add the following placeholders:"),
                new UnorderedList(
                        new ListItem(new Html(
                                "<span><i>%s</i> Recipient's display name</span>"
                                        .formatted(mailSettings.getVarRcptName())
                        )),
                        new ListItem(new Html(
                                "<span><i>%s</i> Sender's display name</span>"
                                        .formatted(mailSettings.getVarSenderName())
                        )),
                        new ListItem(new Html(
                                "<span><i>%s</i> Link to one time URL</span>"
                                        .formatted(mailSettings.getVarOtpAuthUrl())
                        )),
                        new ListItem(new Html(
                                "<span><i>%s</i> URL's End of Life</span>"
                                        .formatted(mailSettings.getVarOtpAuthEolUrl())
                        ))
                )
        );

        HorizontalLayout editorLayout = new HorizontalLayout(
                editorHtml,
                editorPlain,
                help
        );
        editorLayout.setFlexGrow(4, editorHtml);
        editorLayout.setFlexGrow(4, editorPlain);
        editorLayout.setFlexGrow(1, help);
        editorLayout.setWidthFull();

        binder.forField(urlSetupOtpAuth)
                .asRequired()
                .bind(MailSettings::getTemplateOtpAuthUrl, MailSettings::setTemplateOtpAuthUrl);
        binder.forField(otpAuthTemplateType)
                .bind(MailSettings::getTemplateOtpAuthType, MailSettings::setTemplateOtpAuthType);
        binder.forField(editorHtml)
                .asRequired()
                .bind(
                        MailSettings::getTemplateOtpAuthHtml,
                        MailSettings::setTemplateOtpAuthHtml
                );

        sendTestOtpAuthButton = new Button("Send Test Link", e -> {
            Dialog dlg = createSendTestOtpAuthDialog();
            dlg.open();
        });
        resetOtpAuthTemplatesButton = new Button("Load default text", e -> {
            switch (otpAuthTemplateType.getValue()) {
                case HTML ->
                    editorHtml.setValue(
                            mailSettings.getDefaultTemplateOtpAuthHtml()
                    );
                case PLAIN ->
                    editorPlain.setValue(
                            mailSettings.getDefaultTemplateOtpAuthPlain()
                    );
            }
        });
        buttons.add(sendTestOtpAuthButton, resetOtpAuthTemplatesButton);

        otpAuthTemplateType.addValueChangeListener(
                (e) -> {
                    switch (e.getValue()) {
                        case HTML -> {
                            editorHtml.setVisible(true);
                            editorPlain.setVisible(false);
                        }
                        case PLAIN -> {
                            editorHtml.setVisible(false);
                            editorPlain.setVisible(true);
                        }
                    }
                }
        );

        layout.add(
                urlLayout,
                otpAuthTemplateType,
                editorLayout
        );

        layout.setMargin(false);
        layout.setPadding(false);

        return layout;
    }
}
