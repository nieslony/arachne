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
package at.nieslony.arachne.tasks.scheduled;

import at.nieslony.arachne.onetimeview.OneTimeViewModel;
import at.nieslony.arachne.onetimeview.OneTimeViewRepository;
import at.nieslony.arachne.onetimeview.OneTimeViewSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.tasks.RecurringTaskDescription;
import at.nieslony.arachne.tasks.Task;
import at.nieslony.arachne.tasks.TaskDescription;
import at.nieslony.arachne.utils.ArachneTimeUnit;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
@TaskDescription(name = "Delete expired Links to One Time Views")
@RecurringTaskDescription(timeUnit = ArachneTimeUnit.DAY, defaulnterval = 1)
@Slf4j
public class RemoveExpiredOtvLinks extends Task {

    @Override
    public String run(BeanFactory beanFactory) throws Exception {
        OneTimeViewRepository oneTimeViewRepository
                = beanFactory.getBean(OneTimeViewRepository.class);
        Settings settings
                = beanFactory.getBean(Settings.class);
        OneTimeViewSettings oneTimeViewSettings
                = settings.getSettings(OneTimeViewSettings.class);
        int validDays = oneTimeViewSettings.getValidDays();
        int graceDays = oneTimeViewSettings.getRemovalGraceTime();
        long count = oneTimeViewRepository.count();

        LocalDateTime expirationDate = LocalDateTime
                .now()
                .minusDays(validDays + graceDays);

        List<OneTimeViewModel> expiredItems
                = oneTimeViewRepository.findByValidUntilBefore(expirationDate);

        log.info("Removing since %s expired OTVs: %s".formatted(
                expirationDate.toString(),
                expiredItems.toString()
        ));
        oneTimeViewRepository.deleteAll(expiredItems);

        return "%d of %d OTV entries deleted".formatted(
                expiredItems.size(),
                count
        );
    }
}
