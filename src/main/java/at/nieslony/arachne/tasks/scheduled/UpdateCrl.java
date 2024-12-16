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
package at.nieslony.arachne.tasks.scheduled;

import at.nieslony.arachne.openvpn.OpenVpnController;
import at.nieslony.arachne.tasks.RecurringTaskDescription;
import at.nieslony.arachne.tasks.Task;
import at.nieslony.arachne.tasks.TaskDescription;
import at.nieslony.arachne.utils.ArachneTimeUnit;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
@TaskDescription(name = "Update CRL")
@RecurringTaskDescription(timeUnit = ArachneTimeUnit.DAY, defaulnterval = 6)
public class UpdateCrl extends Task {

    @Override
    public String run(BeanFactory beanFactory) {
        OpenVpnController openVpnRestController
                = beanFactory.getBean(OpenVpnController.class);
        openVpnRestController.writeCrl();
        return null;
    }
}
