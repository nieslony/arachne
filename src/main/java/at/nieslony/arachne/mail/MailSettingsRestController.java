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
import at.nieslony.arachne.pki.PkiNotInitializedException;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.ArachneUser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author claas
 */
@RestController
@RequestMapping("/api/mail")
public class MailSettingsRestController {

    @Autowired
    Settings settings;

    @Autowired
    OpenVpnRestController openVpnRestController;

    public MailSettingsRestController() {
    }

    @GetMapping("/")
    @RolesAllowed(value = {"ADMIN"})
    public MailSettings getMailSettings() {
        MailSettings mailSettings = new MailSettings(settings);
        return mailSettings;
    }

    @PostMapping("/")
    @RolesAllowed(value = {"ADMIN"})
    public void postMailSettings(
            @RequestBody MailSettings mailSettings
    ) {
        mailSettings.save(settings);
    }

    public void sendConfigMail(
            MailSettings mailSettings,
            ArachneUser forUser,
            String to,
            String subject
    ) throws IOException, MessagingException, PkiNotInitializedException {
        JavaMailSender mailSender = mailSettings.getMailSender();
        MimeMessage message = mailSender.createMimeMessage();
        String from = mailSettings.getPrettySenderMailAddress();
        OpenVpnUserSettings openVpnUserSettings = new OpenVpnUserSettings(settings);

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
                        .replace(mailSettings.getVarSenderName(), forUser.getDisplayName())
                        .replace(mailSettings.getVarLinuxInstructions(), linuxConfig)
                        .replace(mailSettings.getVarRcptName(), mailSettings.getSenderDisplayname());
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
                        .replace(mailSettings.getVarRcptName(), mailSettings.getSenderDisplayname());
                helper.setText(msg, false);
            }
        }

        mailSender.send(message);
    }
}
