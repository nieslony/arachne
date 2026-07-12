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

import at.nieslony.arachne.settings.Settings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
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

    public static LocalDate createValidUntil(int days, boolean ignoreWeekends) {
        LocalDate result = LocalDate.now();

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
