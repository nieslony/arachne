/*
 * Copyright (C) 2026 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.onetimeview;

import at.nieslony.arachne.mail.MailSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.UserModel;
import com.vaadin.flow.component.Component;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 *
 * @author claas
 */
@Service
@Slf4j
public class OneTimeViewService {

    @Autowired
    OneTimeViewRepository oneTimeViewRepository;

    @Autowired
    Settings settings;

    Map<Class<? extends Component>, String> otvRoutes;

    public OneTimeViewService() {
        otvRoutes = Map.of(SetOtpView.class, "set-otp-token");
    }

    public String getRouteFor(Class<? extends Component> view) {
        return otvRoutes.get(view);
    }

    public OneTimeViewModel createOneTimeView(String username, String view) {
        OneTimeViewSettings oneTimeViewSettings = settings.getSettings(OneTimeViewSettings.class);
        UUID uuid = UUID.randomUUID();
        String id;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(uuid.toString().getBytes(StandardCharsets.UTF_8));
            id = new String(Hex.encode(hash));
        } catch (NoSuchAlgorithmException ex) {
            log.error("Cannot create SHA256: " + ex.getMessage());
            id = null;
        }

        OneTimeViewModel model = new OneTimeViewModel();
        model.setId(id);
        model.setUsername(username);
        model.setView(view);
        model.setValidUntil(createValidUntil(
                oneTimeViewSettings.getValidDays(),
                oneTimeViewSettings.isIgnoreWeekends()
        ));

        return oneTimeViewRepository.save(model);
    }

    public void sendEmail(UserModel user, Class<? extends Component> view)
            throws MessagingException {
        MailSettings mailSettings = settings.getSettings(MailSettings.class);

        String viewRoute = otvRoutes.get(view);
        OneTimeViewModel model = createOneTimeView(user.getUsername(), viewRoute);
        String url = "%s/otv/%s/%s".formatted(
                mailSettings.getTemplateOtpAuthUrl(),
                model.getId(),
                viewRoute
        );

        JavaMailSender sender = mailSettings.getMailSender();
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                true,
                "UTF-8"
        );
        helper.setFrom(mailSettings.getPrettySenderMailAddress());
        helper.setTo(user.getEmail());
        helper.setSubject("Attach OTP Authenticator");
        helper.setText("", true);
        switch (mailSettings.getTemplateOtpAuthType()) {
            case HTML -> {
                String msg = mailSettings.getTemplateOtpAuthHtml()
                        .replace(mailSettings.getVarRcptName(), user.getDisplayName())
                        .replace(mailSettings.getVarSenderName(), mailSettings.getSenderDisplayname())
                        .replace(mailSettings.getVarOtpAuthUrl(), url)
                        .replace(mailSettings.getVarOtpAuthEolUrl(), model.getValidUntilString());
                helper.setText(msg, true);
            }
            case PLAIN -> {
                String msg = mailSettings.getTemplateOtpAuthHtml()
                        .replace(mailSettings.getVarRcptName(), user.getDisplayName())
                        .replace(mailSettings.getVarSenderName(), mailSettings.getSenderDisplayname())
                        .replace(mailSettings.getVarOtpAuthUrl(), url)
                        .replace(mailSettings.getVarOtpAuthEolUrl(), model.getValidUntilString());
                helper.setText(msg, false);
            }
        }

        sender.send(message);
    }

    public static LocalDateTime createValidUntil(int days, boolean ignoreWeekends) {
        LocalDateTime result = LocalDateTime.now();

        if (ignoreWeekends) {
            int addedDays = 0;
            while (addedDays < days) {
                result = result.plusDays(1);
                if (!(result.getDayOfWeek() == DayOfWeek.SATURDAY || result.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                    ++addedDays;
                }
            }
        } else {
            result.plusDays(days);
        }

        return result;
    }
}
