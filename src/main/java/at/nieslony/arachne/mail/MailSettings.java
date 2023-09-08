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

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import at.nieslony.arachne.utils.net.MxRecord;
import at.nieslony.arachne.utils.net.NetUtils;
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
public class MailSettings extends AbstractSettingsGroup {

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

    private String smtpServer = getDefaultSmtpServer();
    private int smtpPort = 25;
    private String smtpUser = "arachne";
    private String smtpPassword = "";
    private String senderDisplayname = "Arachne openVPN Administrator";
    private String senderEmailAddress = "no-reply@" + NetUtils.myDomain();
    private String templateConfigHtml = getDefaultTemplateConfigHtml();
    private String templateConfigPlain = getDefaultTemplateConfigPlain();
    private TemplateConfigType templateConfigType = TemplateConfigType.HTML;

    private String getDefaultSmtpServer() {
        try {
            List<MxRecord> recs = NetUtils.mxLookup();
            if (recs != null && !recs.isEmpty()) {
                return recs.get(0).getValue();
            }
        } catch (NamingException ex) {
            logger.warn("Cannot get MX record: " + ex.getMessage());
        }
        return "mail." + NetUtils.myDomain();
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
        final String RN = "MailTemplates/openvpn-config.html";
        try {
            InputStream is = new ClassPathResource(RN).getInputStream();
            return new String(is.readAllBytes());
        } catch (IOException ex) {
            logger.error("Cannot load resource %s: %s"
                    .formatted(RN, ex.getMessage())
            );
            return "";
        }
    }

    final public String getDefaultTemplateConfigPlain() {
        Source htmlSource = new Source(getDefaultTemplateConfigHtml());
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

    public String getVarAttachnement() {
        return "{attachment}";
    }
}
