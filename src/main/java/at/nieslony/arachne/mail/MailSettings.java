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
import java.util.Properties;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 *
 * @author claas
 */
@Getter
@Setter
public class MailSettings {

    private static final Logger logger = LoggerFactory.getLogger(MailSettings.class);

    enum TemplateConfigType {
        PLAIN("Plain Text"),
        HTML("Html");

        private String typeName;

        private TemplateConfigType(String tn) {
            typeName = tn;
        }

        @Override
        public String toString() {
            return typeName;
        }
    }

    private String smtpServer;
    private int smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private String senderDisplayname;
    private String senderEmailAddress;
    private String templateConfigHtml;
    private String templateConfigPlain;
    private TemplateConfigType templaeConfigType;

    private final static String SK_MAIL_SMTP_SERVER = "mail.smtp-server";
    private final static String SK_MAIL_SMTP_PORT = "mail.smtp-port";
    private final static String SK_MAIL_SMTP_USER = "mail.smtp-user";
    private final static String SK_MAIL_SMTP_PASSWORD = "mail.smtp-password";
    private final static String SK_MAIL_SENDER_DISPLAYNAME = "mail.sender-displayname";
    private final static String SK_MAIL_SENDER_EMAIL_ADDRESS = "mail.sender-email-address";
    private final static String SK_MAIL_TMPL_CFG_HTML = "mail.template-config-html";
    private final static String SK_MAIL_TMPL_CFG_PLAIN = "mail.template-config-plain";
    private final static String SK_MAIL_TMPL_CFG_TYPE = "mail.template-config-type";

    private final static String TEMPLATE_CONFIG_HTML
            = """
              <p>
                  <span style="font-family:Verdana, Geneva, sans-serif;"><strong>Dear {displayname}</strong>,</span>
              </p>
              <p>
                  <span style="font-family:Verdana, Geneva, sans-serif;">please follow the instructions:</span>
              </p>
              <p>
                  <span style="font-family:Verdana, Geneva, sans-serif;"><strong>Windows</strong></span>
              </p>
              <ol>
                  <li>
                      <span style="font-family:Verdana, Geneva, sans-serif;">Download latest openVPN client from </span><a href="https://openvpn.net/community-downloads/"><span style="font-family:Verdana, Geneva, sans-serif;">https://openvpn.net/community-downloads/</span></a>
                  </li>
                  <li>
                      <span style="font-family:Verdana, Geneva, sans-serif;">Copy attached openvpn-client.conf to C:\\Users\\YourUsername</span>
                  </li>
              </ol>
              <p>
                  <span style="font-family:Verdana, Geneva, sans-serif;"><strong>Linux</strong></span>
              </p>
              <ol>
                  <li>
                      <span style="font-family:Verdana, Geneva, sans-serif;">open Terminal (konsole or ...)</span>
                  </li>
                  <li>
                      <span style="font-family:Verdana, Geneva, sans-serif;">execute the following commands:</span>
                  </li>
              </ol>
              <pre><code class="language-plaintext">{instructions}</code></pre>
              <p>
                  Best regards,
              </p>
              <p>
                  {sender}
              </p>
              """;

    private final static String TEMPLATE_CONFIG_PLAIN
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
        templateConfigHtml = settings.get(SK_MAIL_TMPL_CFG_HTML, getDefaultTemplateConfigHtml());
        templateConfigPlain = settings.get(SK_MAIL_TMPL_CFG_PLAIN, getDefaultTemplateConfigPlain());
        templaeConfigType = settings.getEnum(SK_MAIL_TMPL_CFG_TYPE, TemplateConfigType.HTML);
    }

    public void save(Settings settings) {
        settings.put(SK_MAIL_SMTP_SERVER, smtpServer);
        settings.put(SK_MAIL_SMTP_PORT, smtpPort);
        settings.put(SK_MAIL_SMTP_USER, smtpUser);
        settings.put(SK_MAIL_SMTP_PASSWORD, smtpPassword);
        settings.put(SK_MAIL_TMPL_CFG_HTML, templateConfigHtml);
        settings.put(SK_MAIL_TMPL_CFG_PLAIN, templateConfigPlain);
        settings.put(SK_MAIL_TMPL_CFG_TYPE, templaeConfigType);
        settings.put(SK_MAIL_SENDER_DISPLAYNAME, senderDisplayname);
        settings.put(SK_MAIL_SENDER_EMAIL_ADDRESS, senderEmailAddress);
    }

    public JavaMailSender getMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(getSmtpServer());
        mailSender.setPort(getSmtpPort());
        mailSender.setUsername(getSmtpUser());
        mailSender.setPassword(getSmtpPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");
        logger.info("Propertries; " + props.toString());

        return mailSender;
    }

    public String getPrettySenderMailAddress() {
        if ("".equals(senderDisplayname)) {
            return senderEmailAddress;
        } else {
            return "%s <%s>".formatted(senderDisplayname, senderEmailAddress);
        }
    }

    final public String getDefaultTemplateConfigHtml() {
        return TEMPLATE_CONFIG_HTML;
    }

    final public String getDefaultTemplateConfigPlain() {
        return TEMPLATE_CONFIG_PLAIN;
    }
}
