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

import static at.nieslony.arachne.mail.MailSettings.TemplateConfigType.HTML;
import static at.nieslony.arachne.mail.MailSettings.TemplateConfigType.PLAIN;
import at.nieslony.arachne.openvpn.OpenVpnRestController;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.users.UserModel;
import jakarta.annotation.security.RolesAllowed;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 *
 * @author claas
 */
@RestController
@RequestMapping("/api/mail")
public class MailSettingsRestController {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnRestController.class);

    @Autowired
    Settings settings;

    @Autowired
    OpenVpnRestController openVpnRestController;

    public MailSettingsRestController() {
    }

    @GetMapping("/")
    @RolesAllowed(value = {"ADMIN"})
    public MailSettings getMailSettings() throws SettingsException {
        MailSettings mailSettings = settings.getSettings(MailSettings.class);
        return mailSettings;
    }

    @PostMapping("/")
    @RolesAllowed(value = {"ADMIN"})
    public void postMailSettings(@RequestBody MailSettings mailSettings)
            throws SettingsException {
        mailSettings.save(settings);
    }

    public void sendConfigMail(
            MailSettings mailSettings,
            UserModel forUser,
            String to,
            String subject
    ) throws IOException, MessagingException, PkiException, SettingsException {
        JavaMailSender mailSender = mailSettings.getMailSender();
        MimeMessage message = mailSender.createMimeMessage();
        String from = mailSettings.getPrettySenderMailAddress();
        OpenVpnUserSettings openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);

        String windowsConfig = openVpnRestController
                .openVpnUserConfig(forUser.getUsername());
        String linuxConfig = openVpnRestController
                .openVpnUserConfigShell(forUser.getUsername());

        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                true,
                "UTF-8"
        );
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.addAttachment(
                openVpnUserSettings.getClientConfigName(),
                new ByteArrayDataSource(windowsConfig, "text/plain")
        );
        logger.info("sender: " + mailSettings.getSenderDisplayname());
        logger.info("Recipient: " + forUser.getDisplayName());
        logger.info("rcpt var: " + mailSettings.getVarRcptName());
        logger.info("sender var; " + mailSettings.getVarSenderName());
        logger.info("attachment var: " + mailSettings.getVarAttachnement());
        switch (mailSettings.getTemplateConfigType()) {
            case HTML -> {
                String msg = mailSettings
                        .getTemplateConfigHtml()
                        .replace(
                                mailSettings.getVarNmConnection(),
                                openVpnUserSettings.getFormattedClientConfigName(
                                        forUser.getUsername()
                                )
                        )
                        .replace(mailSettings.getVarRcptName(), forUser.getDisplayName())
                        .replace(
                                mailSettings.getVarLinuxInstructions(),
                                HtmlUtils.htmlEscape(linuxConfig)
                        )
                        .replace(mailSettings.getVarSenderName(), mailSettings.getSenderDisplayname())
                        .replace(mailSettings.getVarAttachnement(), openVpnUserSettings.getClientConfigName());
                String style = """
                               <style>
                               code {
                                white-space: pre-wrap;
                                font-family: monospace;
                                column-count: 1;
                                background-color: #dddddd;
                               }
                               </style>
                               """;
                helper.setText(style + msg, true);
            }
            case PLAIN -> {
                String msg = mailSettings
                        .getTemplateConfigPlain()
                        .replace(
                                mailSettings.getVarNmConnection(),
                                openVpnUserSettings.getFormattedClientConfigName(
                                        forUser.getUsername()
                                )
                        )
                        .replace(mailSettings.getVarSenderName(), forUser.getDisplayName())
                        .replace(mailSettings.getVarLinuxInstructions(), linuxConfig)
                        .replace(mailSettings.getVarRcptName(), mailSettings.getSenderDisplayname())
                        .replace(mailSettings.getVarAttachnement(), openVpnUserSettings.getClientConfigName());
                helper.setText(msg, false);
            }
        }

        mailSender.send(message);
    }
}
