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

import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.MxRecord;
import at.nieslony.arachne.utils.NetUtils;
import java.util.List;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Getter
@Setter
public class MailSettings {

    private static final Logger logger = LoggerFactory.getLogger(MailSettings.class);

    private String smtpServer;
    private int smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private String senderDisplayname;
    private String senderEmailAddress;
    private String templateConfig;

    private final static String SK_MAIL_SMTP_SERVER = "mail.smtp-server";
    private final static String SK_MAIL_SMTP_PORT = "mail.smtp-port";
    private final static String SK_MAIL_SMTP_USER = "mail.smtp-user";
    private final static String SK_MAIL_SMTP_PASSWORD = "mail.smtp-password";
    private final static String SK_MAIL_SENDER_DISPLAYNAME = "mail.sender-displayname";
    private final static String SK_MAIL_SENDER_EMAIL_ADDRESS = "mail.sender-email-address";
    private final static String SK_MAIL_TEMPLATE_CONFIG = "mail.template-config";

    private final static String TEMPLATE_CONFIG_DEFAULT
            = """
              Dear {displayname},

              Windows
              -------

                1. Download latest openVPN client from https://openvpn.net/community-downloads/

                2. Copy attached openvpn-client.conf to C:\\Users\\YourUsername

              Linux
              -----

                1. open Terminal (konsole or ...)

                2. execute the following commands:

              Best Regards,
              {sendername}
              """;

    public MailSettings() {
    }

    public MailSettings(Settings settings) {
        String smtpServerDefaultStr = "mail." + NetUtils.myDomain();
        try {
            List<MxRecord> recs = NetUtils.mxLookup();
            if (recs != null && !recs.isEmpty()) {
                smtpServerDefaultStr = recs.get(0).getValue();
            }
        } catch (NamingException ex) {
            logger.warn("Cannot get MX record: " + ex.getMessage());
        }

        smtpServer = settings.get(SK_MAIL_SMTP_SERVER, smtpServerDefaultStr);
        smtpPort = settings.getInt(SK_MAIL_SMTP_PORT, 25);
        smtpUser = settings.get(SK_MAIL_SMTP_USER, "arachne");
        smtpPassword = settings.get(SK_MAIL_SMTP_PASSWORD, "");
        senderDisplayname = settings.get(SK_MAIL_SENDER_DISPLAYNAME, "Arachne openVPN Administrator");
        senderEmailAddress = settings.get(
                SK_MAIL_SENDER_EMAIL_ADDRESS,
                "no-reply@" + NetUtils.myDomain()
        );
        templateConfig = settings.get(SK_MAIL_TEMPLATE_CONFIG, TEMPLATE_CONFIG_DEFAULT);
    }

    public void save(Settings settings) {
        settings.put(SK_MAIL_SMTP_SERVER, smtpServer);
        settings.put(SK_MAIL_SMTP_PORT, smtpPort);
        settings.put(SK_MAIL_SMTP_USER, smtpUser);
        settings.put(SK_MAIL_SMTP_PASSWORD, smtpPassword);
        settings.put(SK_MAIL_TEMPLATE_CONFIG, templateConfig);
        settings.put(SK_MAIL_SENDER_DISPLAYNAME, senderDisplayname);
        settings.put(SK_MAIL_SENDER_EMAIL_ADDRESS, senderEmailAddress);
    }
}
