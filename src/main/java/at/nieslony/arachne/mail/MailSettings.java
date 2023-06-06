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

import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.MxRecord;
import at.nieslony.arachne.utils.NetUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
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
    private TemplateConfigType templateConfigType;

    private final static String SK_MAIL_SMTP_SERVER = "mail.smtp-server";
    private final static String SK_MAIL_SMTP_PORT = "mail.smtp-port";
    private final static String SK_MAIL_SMTP_USER = "mail.smtp-user";
    private final static String SK_MAIL_SMTP_PASSWORD = "mail.smtp-password";
    private final static String SK_MAIL_SENDER_DISPLAYNAME = "mail.sender-displayname";
    private final static String SK_MAIL_SENDER_EMAIL_ADDRESS = "mail.sender-email-address";
    private final static String SK_MAIL_TMPL_CFG_HTML = "mail.template-config-html";
    private final static String SK_MAIL_TMPL_CFG_PLAIN = "mail.template-config-plain";
    private final static String SK_MAIL_TMPL_CFG_TYPE = "mail.template-config-type";

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
        templateConfigHtml = settings.get(SK_MAIL_TMPL_CFG_HTML, getDefaultTemplateConfigHtml(settings));
        templateConfigPlain = settings.get(SK_MAIL_TMPL_CFG_PLAIN, getDefaultTemplateConfigPlain(settings));
        templateConfigType = settings.getEnum(SK_MAIL_TMPL_CFG_TYPE, TemplateConfigType.HTML);
    }

    public void save(Settings settings) {
        settings.put(SK_MAIL_SMTP_SERVER, smtpServer);
        settings.put(SK_MAIL_SMTP_PORT, smtpPort);
        settings.put(SK_MAIL_SMTP_USER, smtpUser);
        settings.put(SK_MAIL_SMTP_PASSWORD, smtpPassword);
        settings.put(SK_MAIL_TMPL_CFG_HTML, templateConfigHtml);
        settings.put(SK_MAIL_TMPL_CFG_PLAIN, templateConfigPlain);
        settings.put(SK_MAIL_TMPL_CFG_TYPE, templateConfigType);
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

    final public String getDefaultTemplateConfigHtml(Settings settings) {
        final String RN = "MailTemplates/openvpn-config.html";
        OpenVpnUserSettings openVpnUserSettings = new OpenVpnUserSettings(settings);
        try {
            InputStream is = new ClassPathResource(RN).getInputStream();
            return new String(is.readAllBytes())
                    .replace("{attachment}", openVpnUserSettings.getClientConfigName());
        } catch (IOException ex) {
            logger.error("Cannot load resource %s: %s"
                    .formatted(RN, ex.getMessage())
            );
            return "";
        }
    }

    final public String getDefaultTemplateConfigPlain(Settings settings) {
        Source htmlSource = new Source(getDefaultTemplateConfigHtml(settings));
        Segment segment = new Segment(htmlSource, 0, htmlSource.length());
        Renderer htmlRender = new Renderer(segment)
                .setMaxLineLength(80)
                .setIncludeHyperlinkURLs(true)
                .setListIndentSize(4);
        return htmlRender.toString();
    }

    public String getVarRcptName() {
        return "{rcpt-displayname}";
    }

    public String getVarSenderName() {
        return "{sndr-displayname}";
    }

    public String getVarLinuxInstructions() {
        return "{linux-instructions}";
    }

    public String getVarNmConnection() {
        return "{nm-connection}";
    }
}
